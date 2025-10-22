// src/pages/GatePage.jsx
import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";
import "../../styles/gate.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export default function GatePage() {
  const navigate = useNavigate();
  const location = useLocation();

  const api = useMemo(
    () =>
      axios.create({
        baseURL: API,
        withCredentials: true,
      }),
    []
  );

  const gno =
    Number(location.state?.gno) ||
    Number(new URLSearchParams(window.location.search).get("gno")) ||
    0;

  const [queued, setQueued] = useState(false);
  const [waitingCount, setWaitingCount] = useState(0);
  const [position, setPosition] = useState(-1);
  const [ready, setReady] = useState(false);

  /** 🟢 대기열 등록 */
  const enqueue = async () => {
    try {
      const token = localStorage.getItem("jwt");
      const { data } = await api.post("/gate/enqueue", gno, {
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });

      console.log("[GatePage] 📥 Enqueue 응답:", data);

      // 🎯 예매 완료자 차단
      if (data.waiting === -1 || data.msg === "이미 예매 완료된 사용자입니다.") {
        alert("이미 예매 완료된 사용자입니다.\n게이트 입장이 제한됩니다.");
        navigate("/home", { replace: true });
        return;
      }

      if (!data.queued) {
        alert("대기열 등록 실패 — 예약이 불가능합니다.");
        navigate("/home", { replace: true });
        return;
      }

      // 정상 등록
      setQueued(true);
      setWaitingCount(data?.waiting ?? 0);
    } catch (e) {
      console.error("[GatePage] ❌ 대기열 등록 실패:", e);
      alert("대기열 등록 중 오류가 발생했습니다.");
      navigate("/home", { replace: true });
    }
  };

  /** 창 닫힘 / 새로고침 시 leave() 호출 (퍼밋 반환 + 로그) */
  useEffect(() => {
    const handleUnload = async () => {
      console.log("[GatePage] 🚪 beforeunload 이벤트 발생 — leave 호출 예정");

      try {
        navigator.sendBeacon?.(
          `${API}/gate/leave?gno=${encodeURIComponent(gno)}`,
          new Blob([], { type: "text/plain" })
        );
        console.log("[GatePage] ✅ sendBeacon 전송 완료 (퍼밋 반환)");
      } catch (e) {
        console.warn("[GatePage] ⚠ sendBeacon 실패 → fetch로 폴백:", e);
        try {
          await fetch(`${API}/gate/leave`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            keepalive: true,
            body: JSON.stringify(gno),
          });
          console.log("[GatePage] ✅ fetch keepalive 성공 (퍼밋 반환)");
        } catch (err) {
          console.error("[GatePage] ❌ leave() 최종 실패:", err);
        }
      }
    };

    window.addEventListener("beforeunload", handleUnload);
    return () => window.removeEventListener("beforeunload", handleUnload);
  }, [gno]);

  /** 최초 진입 시 대기열 등록 */
  useEffect(() => {
    if (gno && !queued) {
      console.log("[GatePage] 🎬 최초 진입 — enqueue 실행");
      enqueue();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [gno]);

  /** 🔁 상태 폴링 (/gate/check/{gno}, /gate/position/{gno}) */
  useEffect(() => {
    if (!queued || !gno) return;

    let fail = 0;
    let timer = null;

    const tick = async () => {
      try {
        const token = localStorage.getItem("jwt");
        const headers = token ? { Authorization: `Bearer ${token}` } : {};
        const [{ data: check }, { data: pos }] = await Promise.all([
          api.get(`/gate/check/${gno}`, { headers }),
          api.get(`/gate/position/${gno}`, { headers }),
        ]);

        console.log("[GatePage] 🔍 폴링 응답:", check, pos);

        setReady(Boolean(check?.ready));
        setPosition(pos?.position ?? -1);
        fail = 0;
        timer = setTimeout(tick, 1000);
      } catch (e) {
        fail = Math.min(fail + 1, 6);
        console.warn("[GatePage] ⚠ 폴링 오류:", e.message);
        const delay = 1000 * (fail + 1);
        timer = setTimeout(tick, delay);
      }
    };

    tick();
    return () => clearTimeout(timer);
  }, [queued, gno, api]);

  /** 입장 완료 시 macro로 이동 */
  useEffect(() => {
    console.log("[GatePage] ✅ useEffect 감시중 | ready =", ready, "gno =", gno);
    if (ready) {
      console.log("[GatePage] 🎯 ready TRUE 감지됨 — 100ms 후 navigate 실행 예정");
      setTimeout(() => {
        console.log("[GatePage] 🚀 navigate('/macro') 실행 직전 (gno =", gno, ")");
        navigate("/macro", { replace: true, state: { gno } });
      }, 100);
    }
  }, [ready, gno, navigate]);

  return (
    <div className="gate-wrap">
      <h1>🎟️ 예매 대기실</h1>
      {queued ? (
        <>
          <p>내 순번: {position}</p>
          <p>대기 인원(등록 시점): {waitingCount}</p>
          <p>ready 상태: {String(ready)}</p>
          {ready && <p>✅ 입장 준비 완료! 매크로 페이지로 이동합니다...</p>}
        </>
      ) : (
        <p>대기열 등록 중...</p>
      )}
    </div>
  );
}
