import React from "react";
import { Outlet } from "react-router-dom";
import Header from "./Header";
import Footer from "./Footer";
import { Box, Container } from "@mui/material";

const Layout = () => {
  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        minHeight: "100vh",
        bgcolor: "#fff",
      }}
    >
      <Header />

      <Container
        maxWidth={false}
        sx={{
          width: "1280px",
          mx: "auto",
          flexGrow: 1,
          mt: 2,
          mb: 4,
        }}
      >
        <Outlet />
      </Container>

      <Footer />
    </Box>
  );
};

export default Layout;
