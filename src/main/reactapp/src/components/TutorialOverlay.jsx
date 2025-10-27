import React, { useLayoutEffect, useState } from "react";
import { createPortal } from "react-dom";
import styles from "../styles/TutorialOverlay.module.css";

export default function TutorialOverlay({ targetId, message, onClose }) {
    const [rect, setRect] = useState(null);

    useLayoutEffect(() => {
        let attempts = 0;

        const findTarget = () => {
            const el = document.getElementById(targetId);

            if (el) {
                const r = el.getBoundingClientRect();
                // fixed 기준에 맞게 scrollY 보정 제거
                const adjusted = {
                    top: r.top,
                    left: r.left,
                    width: r.width,
                    height: r.height,
                    bottom: r.bottom,
                };
                console.log("[Overlay Debug] adjusted rect:", adjusted);
                setRect(adjusted);
            } else if (attempts < 10) {
                attempts++;
                setTimeout(findTarget, 150);
            }
        };

        findTarget();
    }, [targetId]);

    if (!rect) return null;
    const tooltipWidth = 240;
    let tooltipLeft = rect.left + rect.width / 2 - tooltipWidth / 2;

    // 좌우 화면 경계 보정
    if (tooltipLeft < 10) tooltipLeft = 10;
    if (tooltipLeft + tooltipWidth > window.innerWidth - 10)
        tooltipLeft = window.innerWidth - tooltipWidth - 10;

    // 상하 화면 경계 보정
    let tooltipTop = rect.bottom + 20;
    if (tooltipTop + 100 > window.innerHeight) {
        tooltipTop = rect.top - 100;
    }
    return createPortal(
        <div className={styles.tutorialOverlay}>
            <div
                className={styles.tutorialHighlight}
                style={{
                    top: `${rect.top}px`,
                    left: `${rect.left}px`,
                    width: `${rect.width}px`,
                    height: `${rect.height}px`,
                }}
            />
            <div
                className={styles.tutorialTooltip}
                style={{
                    top: `${tooltipTop}px`,
                    left: `${tooltipLeft}px`,
                    width: `${tooltipWidth}px`,
                }}
            >
                <p>{message}</p>
                <button onClick={onClose}>확인</button>
            </div>
        </div>,
        document.body
    );
}
