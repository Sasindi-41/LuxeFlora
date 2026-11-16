import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { signInWithEmailAndPassword, signOut } from "firebase/auth";
import { auth, db } from "../firebase/firebaseConfig";
import { collection, getDocs, query, where } from "firebase/firestore";

import logo from "../assets/app_logo.png";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleLogin = async () => {
    try {
      setError("");

     
      if (!email) return setError("Enter your email");

      const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailPattern.test(email)) {
        return setError("Enter a valid email address");
      }

      if (!password) return setError("Enter your password");

    
      const userCredential = await signInWithEmailAndPassword(
        auth,
        email,
        password
      );

      const userEmail = userCredential.user.email;

     
      const q = query(
        collection(db, "admins"),
        where("email", "==", userEmail)
      );

      const snapshot = await getDocs(q);

      if (snapshot.empty) {
      
        await signOut(auth);
        setError("Access denied. Not an admin.");
        return;
      }

   
      navigate("/dashboard");
    } catch (err: any) {
      setError("Invalid email or password");
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <img src={logo} style={styles.logoImg} />

        <h2 style={styles.title}>Admin Login</h2>

        <label style={styles.label}>Email</label>
        <input
          style={styles.input}
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <label style={styles.label}>Password</label>
        <input
          style={styles.input}
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {error && <p style={styles.error}>{error}</p>}

        <button style={styles.button} onClick={handleLogin}>
          Sign In
        </button>
      </div>
    </div>
  );
}


const styles: any = {
  container: {
    height: "100vh",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    background: "linear-gradient(135deg, #FFF0F4, #FFE4EC, #FFF8F8)",
  },

  card: {
    width: 360,
    padding: 30,
    borderRadius: 22,
    background: "rgba(255,255,255,0.8)",
    backdropFilter: "blur(14px)",
    boxShadow: "0 20px 60px rgba(140,74,93,0.25)",
    textAlign: "center",
  },

  logoImg: {
    width: 65,
    height: 65,
    objectFit: "contain",
    marginBottom: 10,
  },

  title: {
    color: "#8C4A5D",
    marginBottom: 15,
  },

  label: {
    display: "block",
    textAlign: "left",
    fontSize: 12,
    color: "#5B3F46",
    marginBottom: 5,
    marginTop: 10,
  },

  input: {
    width: "100%",
    padding: 12,
    marginBottom: 10,
    borderRadius: 18,
    border: "1px solid #F3DDE1",
    outline: "none",
  },

  button: {
    width: "100%",
    padding: 12,
    borderRadius: 18,
    border: "none",
    marginTop: 10,
    background: "#8C4A5D",
    color: "white",
    fontWeight: "bold",
    cursor: "pointer",
  },

  error: {
    color: "red",
    fontSize: 12,
    marginBottom: 10,
  },
};