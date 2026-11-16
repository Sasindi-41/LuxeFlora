import { useEffect, useState } from "react";
import {
  collection,
  getDocs,
  addDoc,
  deleteDoc,
  doc,
} from "firebase/firestore";
import { db } from "../firebase/firebaseConfig";

import {
  getDownloadURL,
  ref,
  uploadBytes,
} from "firebase/storage";
import { storage } from "../firebase/firebaseConfig";

export default function Category() {
  const [categories, setCategories] = useState<any[]>([]);
  const [name, setName] = useState("");
  const [categoryId, setCategoryId] = useState(""); 
  const [imageFile, setImageFile] = useState<File | null>(null); 
  const [imageUrls, setImageUrls] = useState<any>({}); 

  const categoryRef = collection(db, "categories");

  const loadCategories = async () => {
    const snapshot = await getDocs(categoryRef);

    const data = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...(doc.data() as any),
    }));

    setCategories(data);

    const urls: any = {};

    for (let c of data) {
      const path = c.imageUrl;

      if (!path) {
        urls[c.id] = "https://via.placeholder.com/40";
        continue;
      }

      try {
        if (path.startsWith("http")) {
          urls[c.id] = path;
        } else {
          const url = await getDownloadURL(ref(storage, path));
          urls[c.id] = url;
        }
      } catch {
        urls[c.id] = "https://via.placeholder.com/40";
      }
    }

    setImageUrls(urls);
  };

  useEffect(() => {
    loadCategories();
  }, []);

  const uploadImage = async (): Promise<string> => {
    if (!imageFile) return "";

    const imageRef = ref(
      storage,
      `category-images/${Date.now()}_${imageFile.name}`
    );

    await uploadBytes(imageRef, imageFile);
    return await getDownloadURL(imageRef);
  };

  const addCategory = async () => {
    if (!name.trim() || !categoryId.trim())
      return alert("Enter all fields");

    let imageUrl = "";

    if (imageFile) {
      imageUrl = await uploadImage();
    }

    await addDoc(categoryRef, {
      categoryId,
      name,
      imageUrl,
    });

     alert("Category added successfully!");

    setName("");
    setCategoryId("");
    setImageFile(null);

    loadCategories();
  };

  const deleteCategory = async (id: string) => {
    await deleteDoc(doc(db, "categories", id));
    loadCategories();
  };

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>Category Management</h2>

      
      <div style={styles.wrapper}>

       
        <div style={styles.left}>
          <h3>Add Category</h3>

          <input
            placeholder="Category ID"
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            style={styles.input}
          />

          <input
            placeholder="Category Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={styles.input}
          />

       
          <input
            type="file"
            accept="image/*"
            style={styles.input}
            onChange={(e) => setImageFile(e.target.files?.[0] || null)}
          />

          <button onClick={addCategory} style={styles.addBtn}>
            Add Category
          </button>
        </div>

      
        <div style={styles.right}>
          <h3>Current Categories</h3>

          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Image</th>
                <th style={styles.th}>Category ID</th>
                <th style={styles.th}>Category Name</th>
                <th style={styles.th}>Action</th>
              </tr>
            </thead>

            <tbody>
              {categories.length === 0 ? (
                <tr>
                  <td colSpan={4} style={{ textAlign: "center", padding: 20 }}>
                    No categories found
                  </td>
                </tr>
              ) : (
                categories.map((cat) => (
                  <tr key={cat.id} style={styles.tr}>

                  
                    <td style={styles.td}>
                      <img
                        src={
                          imageUrls[cat.id] ||
                          "https://via.placeholder.com/40"
                        }
                        style={{ width: 40, height: 40, borderRadius: 8 }}
                      />
                    </td>

                    <td style={styles.td}>{cat.categoryId}</td>
                    <td style={styles.td}>{cat.name}</td>

                    <td style={styles.td}>
                      <button
                        onClick={() => deleteCategory(cat.id)}
                        style={styles.deleteBtn}
                      >
                        Delete
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
    width: "35%",
    display: "flex",
    flexDirection: "column",
    gap: 10,
    padding: 20,
    background: "#fff",
    borderRadius: 10,
    boxShadow: "0 0 10px rgba(0,0,0,0.05)",
  },

  right: {
    width: "65%",
    padding: 20,
    background: "#fff",
    borderRadius: 10,
    boxShadow: "0 0 10px rgba(0,0,0,0.05)",
  },

  input: {
    padding: 10,
    border: "1px solid #ddd",
    borderRadius: 8,
    width: "100%",
    outline: "none",
  },

  addBtn: {
    padding: "10px 14px",
    background: "#2A9D8F",
    color: "white",
    border: "none",
    borderRadius: 8,
    cursor: "pointer",
    marginTop: 10,
  },

  table: {
    width: "100%",
    borderCollapse: "collapse",
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
    textAlign: "center",
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