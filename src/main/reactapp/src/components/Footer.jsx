import React, { useEffect, useRef } from "react";
import { Box, Typography } from "@mui/material";

const Footer = () => {
  const footerRef = useRef(null);

  useEffect(() => {
    const applyPadding = () => {
      const h = footerRef.current?.offsetHeight || 0;
      // ✅ 콘텐츠가 풋터에 가리지 않도록 본문 하단 패딩을 풋터 높이만큼 부여
      document.body.style.paddingBottom = `${h}px`;
    };

    // 초기 1회 적용
    applyPadding();

    // 윈도우 리사이즈 시 다시 계산
    window.addEventListener("resize", applyPadding);

    // 풋터 내용 변화(문구/줄바꿈 등)에도 반응
    const ro = new ResizeObserver(applyPadding);
    if (footerRef.current) ro.observe(footerRef.current);

    return () => {
      window.removeEventListener("resize", applyPadding);
      ro.disconnect();
      document.body.style.paddingBottom = ""; // 원복
    };
  }, []);

  return (
    <Box
      ref={footerRef}
      sx={{
        bgcolor: "#111",
        color: "#aaa",
        textAlign: "center",
        py: 3,
        width: "100%",
        position: "fixed",
        bottom: 0,
        left: 0,
        zIndex: (theme) => theme.zIndex.appBar, // 다른 요소보다 위에
      }}
    >
      <Typography variant="body2" sx={{ opacity: 0.8 }}>
        © 2025 Incheon Phoenix Baseball. All Rights Reserved.
      </Typography>
      <Typography variant="caption" sx={{ opacity: 0.6 }}>
        본 서비스는 학습 및 포트폴리오용으로 제작되었습니다.
      </Typography>
    </Box>
  );
};

export default Footer;
