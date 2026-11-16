import { useEffect, useState } from "react";
import {
  collection,
  getDocs,
  doc,
  updateDoc,
} from "firebase/firestore";
import { db } from "../firebase/firebaseConfig";

import { getDownloadURL, ref } from "firebase/storage";
import { storage } from "../firebase/firebaseConfig";

export default function Users() {
  const [users, setUsers] = useState<any[]>([]);
  const [imageCache, setImageCache] = useState<any>({});

  const userRef = collection(db, "users");


  const loadUsers = async () => {
    const snapshot = await getDocs(userRef);

    const data = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...(doc.data() as any),
    })) as any[];

    setUsers(data);

    const cache: any = {};

    for (let user of data) {
      const path = user.profilePicUrl;

      if (!path) {
        cache[user.id] = "https://via.placeholder.com/40";
        continue;
      }

      try {
        let url = "";

        if (path.startsWith("http")) {
          url = path;
        } else if (path.includes("profile-images/")) {
          url = await getDownloadURL(ref(storage, path));
        } else {
          const fullPath = `profile-images/${path}`;
          try {
            url = await getDownloadURL(ref(storage, fullPath));
          } catch {
            url = "https://via.placeholder.com/40";
          }
        }

        cache[user.id] = url;
      } catch {
        cache[user.id] = "https://via.placeholder.com/40";
      }
    }

    setImageCache(cache);
  };

  useEffect(() => {
    loadUsers();
  }, []);

  
  const toggleStatus = async (user: any) => {
    const newStatus = user.status === "active" ? "inactive" : "active";

    await updateDoc(doc(db, "users", user.id), {
      status: newStatus,
    });

    loadUsers();
  };

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>User Management</h2>

   
      <div style={styles.card}>

        <table style={styles.table}>
          <thead>
            <tr style={styles.tr}>
              <th style={styles.th}>Profile</th>
              <th style={styles.th}>Name</th>
              <th style={styles.th}>Email</th>
              <th style={styles.th}>Contact</th>
              <th style={styles.th}>City</th>
              <th style={styles.th}>Address</th>
              <th style={styles.th}>Status</th>
              <th style={styles.th}>Action</th>
            </tr>
          </thead>

          <tbody>
            {users.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: "center", padding: 20 }}>
                  No users found
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id} style={styles.tr}>
                  <td style={styles.td}>
                    <img
                      src={imageCache[user.id] || "https://via.placeholder.com/40"}
                      style={styles.img}
                    />
                  </td>

                  <td style={styles.td}>{user.name}</td>
                  <td style={styles.td}>{user.email}</td>
                  <td style={styles.td}>{user.contactNo || "-"}</td>
                  <td style={styles.td}>{user.city || "-"}</td>

                  <td style={styles.td}>
                    {user.addressLine1
                      ? `${user.addressLine1}, ${user.addressLine2 || ""}`
                      : "-"}
                  </td>

                  <td style={styles.td}>
                    <span
                      style={{
                        padding: "4px 10px",
                        borderRadius: 10,
                        color: "white",
                        background:
                          user.status === "active" ? "#2A9D8F" : "#6C757D",
                        fontSize: 12,
                      }}
                    >
                      {user.status || "inactive"}
                    </span>
                  </td>

                  <td style={styles.td}>
                    <button
                      onClick={() => toggleStatus(user)}
                      style={{
                        padding: "6px 10px",
                        borderRadius: 8,
                        border: "none",
                        cursor: "pointer",
                        background:
                          user.status === "active" ? "#E63946" : "#2A9D8F",
                        color: "white",
                      }}
                    >
                      {user.status === "active" ? "Deactivate" : "Activate"}
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

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

  card: {
    background: "#fff",
    padding: 20,
    borderRadius: 10,
    boxShadow: "0 0 10px rgba(0,0,0,0.05)",
  },

  table: {
    width: "100%",
    fontSize: 14,
    borderCollapse: "collapse",
    border: "1px solid #eee",
    tableLayout: "fixed",
  },

  th: {
    textAlign: "center",
    padding: "12px",
    borderBottom: "2px solid #ddd",
    background: "#fafafa",
  },

  tr: {
    borderBottom: "1px solid #eee",
  },

  td: {
    padding: "8px",
    verticalAlign: "middle",
    wordWrap: "break-word",
  },

  img: {
    width: 40,
    height: 40,
    borderRadius: "50%",
    objectFit: "cover",
  },
};