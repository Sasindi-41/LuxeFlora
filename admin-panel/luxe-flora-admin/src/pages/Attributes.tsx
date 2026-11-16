import { useEffect, useState } from "react";
import {
  collection,
  getDocs,
  doc,
  updateDoc,
} from "firebase/firestore";
import { db } from "../firebase/firebaseConfig";

export default function Attributes() {
  const [attributes, setAttributes] = useState<any[]>([]);
  const [productDocId, setProductDocId] = useState("");

  const [name, setName] = useState("");
  const [type, setType] = useState("text");
  const [valueInput, setValueInput] = useState("");
  const [values, setValues] = useState<string[]>([]);

  const productRef = collection(db, "products");

  const loadAttributes = async () => {
    const snapshot = await getDocs(productRef);

    let attrs: any[] = [];
    let firstDocId = "";

    for (const docSnap of snapshot.docs) {
      firstDocId = docSnap.id;

      const data = docSnap.data() as any;

      if (Array.isArray(data.attributes)) {
        attrs = data.attributes;
      }

      break; 
    }

    setProductDocId(firstDocId);
    setAttributes(attrs);
  };

  useEffect(() => {
    loadAttributes();
  }, []);


  const addValue = () => {
    if (!valueInput.trim()) return;

    if (values.includes(valueInput)) return;

    setValues([...values, valueInput]);
    setValueInput("");
  };

  const removeValue = (val: string) => {
    setValues(values.filter((v) => v !== val));
  };

 
  const addAttribute = async () => {
    if (!name.trim()) return alert("Enter attribute name");
    if (values.length === 0) return alert("Add values");

    const newAttr = {
      name,
      type,
      values,
    };

    const productDocRef = doc(db, "products", productDocId);

    const updatedAttributes = [...attributes, newAttr];

    await updateDoc(productDocRef, {
      attributes: updatedAttributes,
    });


    alert("Attribute added successfully!");

    setName("");
    setType("text");
    setValues([]);

    loadAttributes();
  };

  
  const deleteAttribute = async (index: number) => {
    const confirmDelete = window.confirm("Are you sure you want to delete this attribute?");
    if (!confirmDelete) return;

    const productDocRef = doc(db, "products", productDocId);

    const updatedAttributes = attributes.filter((_, i) => i !== index);

    await updateDoc(productDocRef, {
      attributes: updatedAttributes,
    });

    loadAttributes();
  };

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>Attributes Management</h2>

      <div style={styles.wrapper}>

      
        <div style={styles.left}>
          <h3>Add Attribute</h3>

          <input
            placeholder="Attribute Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={styles.input}
          />

          <input
            placeholder="Enter type (e.g. text, color, size)"
            value={type}
            onChange={(e) => setType(e.target.value)}
            style={styles.input}
          />

          <div style={{ display: "flex", gap: 10 }}>
            <input
              placeholder="Add value"
              value={valueInput}
              onChange={(e) => setValueInput(e.target.value)}
              style={styles.input}
            />

            <button onClick={addValue} style={styles.addValueBtn}>
              Add
            </button>
          </div>

      
          <div style={styles.tagContainer}>
            {values.map((v, i) => (
              <span key={i} style={styles.tag}>
                {type === "color" && (
                  <span
                    style={{
                      width: 10,
                      height: 10,
                      background: v,
                      display: "inline-block",
                      borderRadius: "50%",
                      marginRight: 5,
                    }}
                  />
                )}
                {v}
                <span onClick={() => removeValue(v)}> ×</span>
              </span>
            ))}
          </div>

          <button onClick={addAttribute} style={styles.addBtn}>
            Save Attribute
          </button>
        </div>

    
        <div style={styles.right}>
          <h3>Current Attributes</h3>

          <table style={styles.table}>
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Values</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {attributes.length === 0 ? (
                <tr>
                  <td colSpan={4} style={{ textAlign: "center", padding: 20 }}>
                    No attributes found
                  </td>
                </tr>
              ) : (
                attributes.map((attr, i) => (
                  <tr key={i}>
                    <td>{attr.name}</td>
                    <td>{attr.type}</td>
                    <td>
                      {attr.values?.map((v: string, j: number) => (
                        <span key={j} style={styles.tag}>
                          {attr.type === "color" && (
                            <span
                              style={{
                                width: 10,
                                height: 10,
                                background: v,
                                display: "inline-block",
                                borderRadius: "50%",
                                marginRight: 5,
                              }}
                            />
                          )}
                          {v}
                        </span>
                      ))}
                    </td>

                    <td>
                      <button
                        onClick={() => deleteAttribute(i)}
                        style={{
                          background: "#E63946",
                          color: "white",
                          border: "none",
                          padding: "6px 10px",
                          borderRadius: 6,
                          cursor: "pointer",
                        }}
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
  },

  addValueBtn: {
    padding: "10px 12px",
    background: "#8C4A5D",
    color: "white",
    border: "none",
    width: 120,
    borderRadius: 8,
  },

  addBtn: {
    padding: "10px",
    background: "#2A9D8F",
    color: "white",
    border: "none",
    borderRadius: 8,
    marginTop: 10,
  },

  tagContainer: {
    display: "flex",
    flexWrap: "wrap",
    gap: 8,
  },

  tag: {
    background: "#EEE",
    padding: "4px 8px",
    borderRadius: 12,
    fontSize: 12,
    display: "flex",
    alignItems: "center",
  },

  table: {
    width: "100%",
    fontSize: 14,
    borderCollapse: "collapse",
  },
};