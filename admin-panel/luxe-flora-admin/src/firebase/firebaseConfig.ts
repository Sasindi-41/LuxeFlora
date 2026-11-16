import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage"; 

const firebaseConfig = {
  apiKey: "AIzaSyBrGzPFq3odaM4aa4QgXwiGi0G5Vd3xht0",
  authDomain: "luxeflora.firebaseapp.com",
  projectId: "luxeflora",
  storageBucket: "luxeflora.firebasestorage.app",
  messagingSenderId: "1047273954299",
  appId: "1:1047273954299:web:c267ebd7d774a5566dc95e",
  measurementId: "G-40P3629Z8J"
};

export const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app);

export const storage = getStorage(app);

import { getAnalytics, isSupported } from "firebase/analytics";

isSupported().then((yes) => {
  if (yes) getAnalytics(app);
});