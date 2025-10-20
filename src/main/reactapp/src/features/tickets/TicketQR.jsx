import React, { useEffect, useState } from "react";
import axios from "axios";

const BASE_URL = "http://localhost:8080/tickets";

export default function TicketQR() {
  const [token, setToken] = useState(localStorage.getItem("accessToken") || "");
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [query, setQuery] = useState("");

  const fetchQrs = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.get(`${BASE_URL}/print`, {
        headers: { Authorization: `Bearer ${token}` },
        withCredentials: true,
      });
      if (Array.isArray(res.data)) setData(res.data);
      else throw new Error("서버 응답 형식이 배열이 아닙니다.");
    } catch (err) {
      console.error(err);
      setError(err.message || "서버 오류");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (token) fetchQrs();
  }, [token]);

  return (
    <div className="p-6 min-h-screen bg-gray-50">
      <h1 className="text-2xl font-bold mb-4">회원별 QR 코드 조회</h1>

      <div className="flex gap-2 mb-4">
        <input
          type="text"
          value={token}
          onChange={(e) => setToken(e.target.value)}
          placeholder="JWT 토큰 입력"
          className="border rounded px-3 py-2 w-96"
        />
        <button
          onClick={() => {
            localStorage.setItem("accessToken", token);
            fetchQrs();
          }}
          className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700"
        >
          조회
        </button>
      </div>

      {error && <p className="text-red-500 mb-3">{error}</p>}

      {loading ? (
        <p>불러오는 중...</p>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
          {data.map((url, idx) => (
            <div key={idx} className="border p-3 rounded-lg bg-white">
              <img
                src={url}
                alt={`QR-${idx}`}
                className="w-full h-48 object-contain rounded"
              />
              <p className="text-xs break-all mt-2">{url}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
