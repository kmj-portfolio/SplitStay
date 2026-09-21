import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
    vus: 35,
    duration: '10m',

    thresholds: {
        'http_req_failed{endpoint:hotel_search}': [
            'rate<0.01'
        ],
        'http_req_duration{endpoint:hotel_search}': [
            'p(95)<300'
        ],
    },
};

const BASE_URL = 'http://localhost:8080';

export default function () {

    const payload = {
        checkIn: '2025-01-10',
        checkOut: '2025-01-11',
        latitude: 37.5007861,
        longitude: 127.0368861,
    };

    const res = http.post(
        `${BASE_URL}/api/hotels/search`,
        JSON.stringify(payload),
        {
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
            },
            tags: {
                endpoint: 'hotel_search',
            },
        }
    );

    check(res, {
        '200 OK': (r) => r.status === 200,
    });

    sleep(1);
}