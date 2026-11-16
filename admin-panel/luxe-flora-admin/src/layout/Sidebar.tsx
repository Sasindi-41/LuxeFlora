import { Link } from "react-router-dom";


import logo from "../assets/app_logo.png";

export default function Sidebar() {
  return (
    <div style={styles.sidebar}>
   
      <div style={styles.logoBox}>
        <img src={logo} style={styles.logoImg} />
     
      </div>

   
      <div style={styles.section}>
        <Link to="/dashboard" style={styles.link}>Dashboard</Link>
        <Link to="/products" style={styles.link}>Products</Link>
        <Link to="/orders" style={styles.link}>Orders</Link>
      </div>

      <div style={styles.divider}></div>

      
      <div style={styles.section}>
        <Link to="/users" style={styles.link}>Users</Link>
        <Link to="/admins" style={styles.link}>Admins</Link>
      </div>

      <div style={styles.divider}></div>

      
      <div style={styles.section}>
        <Link to="/category" style={styles.link}>Categories</Link>
        <Link to="/attributes" style={styles.link}>Attributes</Link>
        <Link to="/chats" style={styles.link}>Chats</Link>
      </div>

      <div style={styles.divider}></div>

     
      <Link to="/" style={styles.link}>Logout</Link>
    </div>
  );
}


const styles: any = {
  sidebar: {
    width: "240px",
    height: "100vh",
    background: "linear-gradient(180deg, #8C4A5D, #B56B7A)",
    color: "white",
    padding: "20px",
    position: "fixed",
    left: 0,
    top: 0,
    display: "flex",
    flexDirection: "column",
  },

  logoBox: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    marginBottom: "25px",
  },

  logoImg: {
    width: "100px",
    height: "100px",
    objectFit: "contain",
    borderRadius: "8px",
    background: "white",
    padding: "3px",
    marginLeft: "70px",
  },

  logoText: {
    fontSize: "18px",
    fontWeight: "bold",
    margin: 0,
  },

  section: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },

  link: {
    color: "white",
    textDecoration: "none",
    padding: "10px",
    borderRadius: "10px",
    fontSize: "14px",
    opacity: 0.9,
  },

  divider: {
    height: "1px",
    background: "rgba(255,255,255,0.2)",
    margin: "15px 0",
  },

  logout: {
    marginTop: "auto",
    color: "white",
    textDecoration: "none",
    padding: "10px",
    borderRadius: "10px",
    background: "rgba(255,255,255,0.15)",
    textAlign: "center",
  },
};