import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = { vus: 5, duration: '1m' };

const BASE_URL = 'http://localhost:8080';

export default function () {
    const res = http.post(`${BASE_URL}/api/hotels/search`,
        JSON.stringify({
            checkIn: '2025-01-10',
            checkOut: '2025-01-11',
            latitude: 37.5007861,
            longitude: 127.0368861,
        }),
        { headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' } }
    );

    check(res, { '200 OK': (r) => r.status === 200 });
    sleep(1);
}
