"""
구 스키마용
"""

import argparse
import math
import os
from datetime import datetime

import numpy as np
import pandas as pd
from scipy.stats import truncnorm

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STD_CSV = os.path.join(SCRIPT_DIR, "hotels_with_std.csv")
DUMMY_CSV = os.path.join(SCRIPT_DIR, "dummy_hotels_100k.csv")
OUTPUT_DIR = os.path.join(SCRIPT_DIR, "old_schema")

TARGET_TOTAL = 100_000
MAX_SIGMA = 3
SEED = 42
NULL = r"\N"

ROOM_TYPES = [
    ("STANDARD", 2, 80_000),
    ("DELUXE", 3, 120_000),
    ("FAMILY", 4, 180_000),
]

def generate_dummies(df: pd.DataFrame, target_total: int, max_sigma: float = MAX_SIGMA) -> pd.DataFrame:
    n_per_hotel = target_total // len(df)
    rows = []
    for base_idx, row in enumerate(df.itertuples(index=False)):
        sigma_lat = row.std_m / 111_320
        sigma_lng = row.std_m / (111_320 * math.cos(math.radians(row.lat)))

        lat_offsets = truncnorm.rvs(-max_sigma, max_sigma, scale=sigma_lat, size=n_per_hotel)
        lng_offsets = truncnorm.rvs(-max_sigma, max_sigma, scale=sigma_lng, size=n_per_hotel)

        for dlat, dlng in zip(lat_offsets, lng_offsets):
            rows.append({
                "base_idx": base_idx,
                "dummy_lat": round(row.lat + dlat, 7),
                "dummy_lng": round(row.lng + dlng, 7),
            })
    return pd.DataFrame(rows)


def load_dummies(base: pd.DataFrame) -> pd.DataFrame:
    if os.path.exists(DUMMY_CSV):
        print(f"3단계: {os.path.basename(DUMMY_CSV)} 발견 → 좌표 재사용")
        dummy = pd.read_csv(DUMMY_CSV, encoding="utf-8-sig")
        n_per_hotel = len(dummy) // len(base)
        if n_per_hotel * len(base) != len(dummy):
            raise ValueError(f"{DUMMY_CSV} 행 수({len(dummy)})가 원본 호텔 수({len(base)})의 배수가 아님")
        dummy["base_idx"] = np.arange(len(dummy)) // n_per_hotel
        return dummy[["base_idx", "dummy_lat", "dummy_lng"]]

    print(f"3단계: truncated normal(±{MAX_SIGMA}σ)로 {TARGET_TOTAL}개 더미 생성")
    return generate_dummies(base, TARGET_TOTAL)


def build_tables(base: pd.DataFrame, dummy: pd.DataFrame, rooms_per_hotel: int,
                 id_start: int, rng: np.random.Generator) -> dict[str, pd.DataFrame]:
    n = len(dummy)
    ids = np.arange(id_start, id_start + n)  # user_id = provider_id = hotel_id
    src = base.iloc[dummy["base_idx"].to_numpy()].reset_index(drop=True)
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    users = pd.DataFrame({
        "user_id": ids,
        "email": [f"dummy_provider_{i}@loadtest.local" for i in ids],
        "password": NULL,
        "role": "PROVIDER",
        "login_source": "LOCAL",
        "social_id": NULL,
        "created_at": now,
        "modified_at": NULL,
        "deleted_at": NULL,
    })

    # provider_id = user_id (@MapsId)
    providers = pd.DataFrame({"provider_id": ids, "hotel_id": ids})

    star_level = src["결정 등급"].str.extract(r"(\d)")[0].astype(float)
    star_level = star_level.fillna(pd.Series(rng.integers(1, 6, n))).astype(int)
    seq_in_base = dummy.groupby("base_idx").cumcount() + 1

    hotels = pd.DataFrame({
        "hotel_id": ids,
        "provider_id": ids,
        "name": src["호텔명"].str.cat(seq_in_base.astype(str), sep=" #"),
        "address": src["주소"],
        "latitude": dummy["dummy_lat"].map(lambda v: f"{v:.7f}"),
        "longitude": dummy["dummy_lng"].map(lambda v: f"{v:.7f}"),
        "description": NULL,
        "star_level": star_level,
        "rating": np.round(rng.uniform(3.0, 5.0, n), 1),
        "review_count": rng.integers(0, 500, n),
    })

    room_types = ROOM_TYPES[:rooms_per_hotel]
    rooms = pd.DataFrame({
        "hotel_id": np.repeat(ids, len(room_types)),
        "room_type": [t for t, _, _ in room_types] * n,
        "max_occupancy": [o for _, o, _ in room_types] * n,
        "price": np.array([p for _, _, p in room_types] * n)
                 + rng.integers(0, 11, n * len(room_types)) * 5_000,
        "description": NULL,
        "total_quantity": rng.integers(5, 21, n * len(room_types)),
    })

    return {
        "user_entity": users,
        "provider_entity": providers,
        "hotel_entity": hotels,
        "room_entity": rooms,
    }


def write_load_sql(tables: dict[str, pd.DataFrame], path: str):
    lines = [
        "-- generate_dummy_data_old_schema.py 로 생성됨",
        "-- 실행: mysql --local-infile=1 -u root -p hotel_db < scripts/old_schema/load_old_schema.sql",
        "SET GLOBAL local_infile = 1;",
        "SET FOREIGN_KEY_CHECKS = 0;  -- provider_entity <-> hotel_entity 순환 FK",
        "SET UNIQUE_CHECKS = 0;",
        "",
    ]
    for table, df in tables.items():
        csv_path = os.path.join(OUTPUT_DIR, f"{table}.csv").replace("\\", "/")
        lines += [
            f"LOAD DATA LOCAL INFILE '{csv_path}'",
            f"INTO TABLE {table} CHARACTER SET utf8mb4",
            "FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'",
            "LINES TERMINATED BY '\\n'",
            "IGNORE 1 LINES",
            f"({', '.join(df.columns)});",
            "",
        ]
    lines += [
        "SET UNIQUE_CHECKS = 1;",
        "SET FOREIGN_KEY_CHECKS = 1;",
        "",
        f"ANALYZE TABLE {', '.join(tables)};",
        "",
        "SELECT COUNT(*) AS hotels FROM hotel_entity;",
        "SELECT COUNT(*) AS rooms  FROM room_entity;",
        "",
    ]
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))


# ── 실행 ──────────────────────────────────────────────────────
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--rooms-per-hotel", type=int, default=len(ROOM_TYPES), choices=range(1, len(ROOM_TYPES) + 1),
                        help="호텔당 방 개수 (STANDARD/DELUXE/FAMILY 순으로 사용)")
    parser.add_argument("--id-start", type=int, default=1,
                        help="user/provider/hotel id 시작값 (기존 데이터가 있으면 MAX(id)+1 로 지정)")
    args = parser.parse_args()

    if not os.path.exists(STD_CSV):
        raise SystemExit(f"{STD_CSV} 없음 → generate_dummy_data.py 로 1~2단계를 먼저 실행")
    print("1~2단계: hotels_with_std.csv 발견 → 건너뜀")
    base = pd.read_csv(STD_CSV, encoding="utf-8-sig")

    rng = np.random.default_rng(SEED)
    np.random.seed(SEED)
    dummy = load_dummies(base)

    print(f"4단계: 구 스키마 CSV 생성 (호텔당 방 {args.rooms_per_hotel}개, id {args.id_start}부터)")
    tables = build_tables(base, dummy, args.rooms_per_hotel, args.id_start, rng)

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    for table, df in tables.items():
        out = os.path.join(OUTPUT_DIR, f"{table}.csv")
        df.to_csv(out, index=False, encoding="utf-8", lineterminator="\n")
        print(f"  → {os.path.relpath(out, SCRIPT_DIR)} ({len(df):,}행)")

    sql_path = os.path.join(OUTPUT_DIR, "load_old_schema.sql")
    write_load_sql(tables, sql_path)
    print(f"  → {os.path.relpath(sql_path, SCRIPT_DIR)}")
    print("완료")
