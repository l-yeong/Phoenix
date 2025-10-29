// public/firebase-messaging-sw.js
importScripts("https://www.gstatic.com/firebasejs/12.4.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/12.4.0/firebase-messaging-compat.js");

// Firebase 초기화 (위 firebaseConfig와 동일해야 함)
firebase.initializeApp({
  apiKey: "",
  authDomain: "",
  projectId: "",
  storageBucket: "",
  messagingSenderId: "",
  appId: "",
  measurementId: ""
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const title = payload?.notification?.title || "알림";
  const body  = payload?.notification?.body  || "";
  const options = {
    body,
    icon: "/icon-192.png", // 선택
    data: payload?.data || {},
  };
  self.registration.showNotification(title, options);
});