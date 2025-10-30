// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getMessaging } from "firebase/messaging";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "",
  authDomain: "",
  projectId: "",
  storageBucket: "",
  messagingSenderId: "",
  appId: "",
  measurementId: ""
};

const app = initializeApp(firebaseConfig);
export const messaging = getMessaging(app);

export async function ensureFcmReady() {
  try {
    // 서비스워커 등록 (public/firebase-messaging-sw.js)
    if ("serviceWorker" in navigator) {
      await navigator.serviceWorker.register("/firebase-messaging-sw.js");
    }

    // 알림 권한
    const perm = await Notification.requestPermission();
    if (perm !== "granted") return null;

    // 🔧 VAPID 공개키(콘솔 > Web Push 인증서)
    const vapidKey = "YOUR_WEB_PUSH_CERTIFICATE_KEY_PAIR_PUBLIC_KEY";

    const token = await getToken(messaging, { vapidKey });
    console.log("[FCM] token:", token);
    return token ?? null;
  } catch (e) {
    console.log("[FCM] init error:", e);
    return null;
  }
}

// 포그라운드 수신 → 직접 UI 표시(토스트 등)
export function attachForegroundHandler(show) {
  onMessage(messaging, (payload) => {
    const title = payload?.notification?.title || "알림";
    const body  = payload?.notification?.body  || "";
    show(title, body, payload?.data || {});
  });
}
