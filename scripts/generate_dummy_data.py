"""
전국 호텔 더미 데이터 생성

1. 836개 실제 호텔 주소 -> 지오코딩 (Nominatim)
2. 각 호텔의 가장 가까운 다른 호텔까지의 거리 계산 (distance to nearest neighbor)
3. std = distance to nearest neighbor / 3 (3-sigma 규칙: 3 sigma 이상이 나올 확률은 0.27%로 희박하다)
4. truncated normal을 사용해 +- 3 sigma를 초과하지 않도록 한다
5. 10만개의 가짜 호텔을 생성한다 (10만 / 863개 실제 호텔 = 116개씩 배분)

"""

import requests
import time
import math
import numpy as np
import pandas as pd
from scipy.stats import truncnorm
from sklearn.neighbors import NearestNeighbors

INPUT_CSV = "hotels_in_south_korea.csv"
OUTPUT_CSV = "dummy_hotels_100k.csv"
TARGET_TOTAL = 100_000
MAX_SIGMA = 3
GEOCODE_DELAY_SEC = 1

# 1) Geocoding
def geocode(address: str):
    # Nominatim으로 주소 -> (lat, lng) 변환, 실패 시 None
    url = "https://nominatim.openstreetmap.org/search"
    params = {"q": address, "format": "json", "limit": 1, "countrycodes": "kr"}
    headers = {"User-Agent": "hotel-dummy-data-generator"}
    try:
        res = requests.get(url, params=params, headers=headers, timeout=10).json()
        if not res:
            return None
        return float(res[0]["lat"]), float(res[0]["lon"])
    except Exception:
        return None


def geocode_all(df: pd.DataFrame) -> pd.DataFrame:
    lats, lngs = [], []
    for i, row in df.iterrows():
        coord = geocode(row["주소"])
        lats.append(coord[0] if coord else None)
        lngs.append(coord[1] if coord else None)
        if (i + 1) % 50 == 0:
            print(f"  지오코딩 진행: {i + 1}/{len(df)}")
        time.sleep(GEOCODE_DELAY_SEC)
    df = df.copy()
    df["lat"], df["lng"] = lats, lngs
    n_before = len(df)
    df = df.dropna(subset=["lat", "lng"])
    print(f"지오코딩 완료: {len(df)}/{n_before}건 성공")
    return df

# 2) 호텔 별로 distance to nearest neighbor 구해 std로 저장
def haversine_m(p1, p2):
    R = 6371000
    lat1, lng1 = np.radians(p1)
    lat2, lng2 = np.radians(p2)
    dlat, dlng = lat2 - lat1, lng2 - lng1
    a = np.sin(dlat / 2) ** 2 + np.cos(lat1) * np.cos(lat2) * np.sin(dlng / 2) ** 2
    return 2 * R * np.arcsin(np.sqrt(a))

def compute_std_per_hotel(df: pd.DataFrame) -> pd.DataFrame:
    coords = df[["lat", "lng"]].values
    nn = NearestNeighbors(n_neighbors=2).fit(np.radians(coords))
    # haversine 거리로 정확히 재계산 (sklearn의 유클리드 근사 대신)
    nn_dist_m = []
    for i, (lat, lng) in enumerate(coords):
        # 후보 압축: sklearn으로 가까운 후보 몇 개만 뽑고 haversine으로 정확히 계산
        dists, idxs = nn.kneighbors([np.radians([lat, lng])], n_neighbors=min(5, len(coords)))
        candidates = [j for j in idxs[0] if j != i]
        real_dists = [haversine_m((lat, lng), tuple(coords[j])) for j in candidates]
        nn_dist_m.append(min(real_dists) if real_dists else 1000.0)  # 고립된 지점은 기본 1km
    df = df.copy()
    df["nn_dist_m"] = nn_dist_m
    df["std_m"] = df["nn_dist_m"] / 3  # 3-sigma 규칙
    return df


# 3) truncated normal을 사용해 max spread를 설정하고, jitter로 더미 생성
def generate_dummies(df: pd.DataFrame, target_total: int, max_sigma: float = MAX_SIGMA) -> pd.DataFrame:
    n_per_hotel = target_total // len(df)
    rows = []
    for _, row in df.iterrows():
        sigma_lat = row["std_m"] / 111_320
        sigma_lng = row["std_m"] / (111_320 * math.cos(math.radians(row["lat"])))

        lat_offsets = truncnorm.rvs(-max_sigma, max_sigma, scale=sigma_lat, size=n_per_hotel)
        lng_offsets = truncnorm.rvs(-max_sigma, max_sigma, scale=sigma_lng, size=n_per_hotel)

        for dlat, dlng in zip(lat_offsets, lng_offsets):
            rows.append({
                "dummy_lat": round(row["lat"] + dlat, 7),
                "dummy_lng": round(row["lng"] + dlng, 7),
                "location_wkt": f"POINT({row['lng'] + dlng:.7f} {row['lat'] + dlat:.7f})",
                "std_m_used": round(row["std_m"], 1),
            })
    return pd.DataFrame(rows)


# ── 실행 ──────────────────────────────────────────────────────
if __name__ == "__main__":
    import os

    # ── 1단계: 지오코딩 ──────────────────────────────
    GEOCODED_CSV = "hotels_geocoded.csv"

    if os.path.exists(GEOCODED_CSV):
        # 이미 완료된 경우 건너뜀
        print("1단계: 지오코딩 결과 파일 발견 → 건너뜀")
        df = pd.read_csv(GEOCODED_CSV)
    else:
        print("1단계: 지오코딩 시작 (약 15분 소요)")
        df = pd.read_csv(INPUT_CSV)
        df = geocode_all(df)
        df.to_csv(GEOCODED_CSV, index=False, encoding="utf-8-sig")
        print(f"  → {GEOCODED_CSV} 저장 완료")

    # ── 2단계: std 계산 ──────────────────────────────
    STD_CSV = "hotels_with_std.csv"

    if os.path.exists(STD_CSV):
        print("2단계: std 계산 결과 파일 발견 → 건너뜀")
        df = pd.read_csv(STD_CSV)
    else:
        print("2단계: nearest neighbor distance 및 std 계산")
        df = compute_std_per_hotel(df)
        df.to_csv(STD_CSV, index=False, encoding="utf-8-sig")
        print(f"  → {STD_CSV} 저장 완료")

        # ── 3단계: 더미 생성 ──────────────────────────────
        print(f"3단계: truncated normal(±{MAX_SIGMA}σ)로 {TARGET_TOTAL}개 더미 생성")
        dummy_df = generate_dummies(df, TARGET_TOTAL)
        dummy_df.to_csv(OUTPUT_CSV, index=False, encoding="utf-8-sig")
        print(f"완료: {len(dummy_df)}개 → {OUTPUT_CSV}")

