import { useEffect, useState } from "react";
import {
  collection,
  getDocs,
  addDoc,
  deleteDoc,
  doc,
  serverTimestamp,
} from "firebase/firestore";
import { db } from "../firebase/firebaseConfig";

export default function Admins() {
  const [admins, setAdmins] = useState<any[]>([]);


  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [mobile, setMobile] = useState("");

  const adminRef = collection(db, "admins");

  const loadAdmins = async () => {
    const snapshot = await getDocs(adminRef);

    const data = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...(doc.data() as any),
    }));

    setAdmins(data);
  };

  useEffect(() => {
    loadAdmins();
  }, []);

 
  const addAdmin = async () => {
    if (!name || !email || !mobile) return alert("Fill all fields");

    await addDoc(adminRef, {
      name,
      email,
      mobile,
      createdAt: serverTimestamp(),
    });
     alert("Admin added successfully!");

    setName("");
    setEmail("");
    setMobile("");

    loadAdmins();
  };


  const deleteAdmin = async (id: string) => {
    await deleteDoc(doc(db, "admins", id));
    loadAdmins();
  };

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>Admin Management</h2>

    
      <div style={styles.wrapper}>

    
        <div style={styles.left}>
          <h3>Add Admin</h3>

          <input
            placeholder="Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={styles.input}
          />

          <input
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            style={styles.input}
          />

          <input
            placeholder="Mobile"
            value={mobile}
            onChange={(e) => setMobile(e.target.value)}
            style={styles.input}
          />

          <button onClick={addAdmin} style={styles.addBtn}>
            Add Admin
          </button>
        </div>

  
        <div style={styles.right}>
          <h3>Current Admins</h3>

          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Name</th>
                <th style={styles.th}>Email</th>
                <th style={styles.th}>Mobile</th>
                <th style={styles.th}>Created At</th>
                <th style={styles.th}>Action</th>
              </tr>
            </thead>

            <tbody>
              {admins.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: "center", padding: 20 }}>
                    No admins found
                  </td>
                </tr>
              ) : (
                admins.map((admin) => (
                  <tr key={admin.id} style={styles.tr}>
                    <td style={styles.td}>{admin.name}</td>
                    <td style={styles.td}>{admin.email}</td>
                    <td style={styles.td}>{admin.mobile}</td>

                    <td style={styles.td}>
                      {admin.createdAt?.seconds
                        ? new Date(admin.createdAt.seconds * 1000).toLocaleString()
                        : "-"}
                    </td>

                    <td style={styles.td}>
                      <button
                        onClick={() => deleteAdmin(admin.id)}
                        style={styles.deleteBtn}
                      >
                        Remove
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

        </div>
      </div>
    </div>
  );
}

const styles: any = {
  container: {
    marginLeft: "100px",
    marginTop: "70px",
    padding: 20,
  },

  title: {
    marginBottom: "50px",
  },

  wrapper: {
    display: "flex",
    gap: 40,
    alignItems: "flex-start",
  },

  left: {
    width: "30%",
    padding: 20,
    background: "#fff",
    borderRadius: 10,
    boxShadow: "0 0 10px rgba(0,0,0,0.05)",
    display: "flex",
    flexDirection: "column",
    gap: 10,
  },

  right: {
    width: "70%",
    padding: 20,
    background: "#fff",
    borderRadius: 10,
    boxShadow: "0 0 10px rgba(0,0,0,0.05)",
  },

  input: {
    padding: 8,
    border: "1px solid #ddd",
    borderRadius: 6,
  },

  addBtn: {
    padding: "8px 12px",
    background: "#2A9D8F",
    color: "white",
    border: "none",
    borderRadius: 6,
    cursor: "pointer",
    marginTop: 10,
  },

  table: {
    width: "100%",
    borderCollapse: "collapse",
    background: "white",
    fontSize: 14,
  },

  th: {
    textAlign: "center",
    padding: 12,
    background: "#f5f5f5",
    borderBottom: "2px solid #ddd",
  },

  tr: {
    borderBottom: "1px solid #eee",
  },

  td: {
    padding: 12,
  },

  deleteBtn: {
    padding: "6px 10px",
    background: "#E63946",
    color: "white",
    border: "none",
    borderRadius: 6,
    cursor: "pointer",
  },
};