
import { useEffect, useState } from "react";
import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  getDocs,
  updateDoc,
} from "firebase/firestore";
import { db } from "../firebase/firebaseConfig";

import {
  getDownloadURL,
  ref,
  uploadBytes,
} from "firebase/storage";
import { storage } from "../firebase/firebaseConfig";
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";

export default function Products() {
  const [products, setProducts] = useState<any[]>([]);
  const [categories, setCategories] = useState<any[]>([]);
  const [editingId, setEditingId] = useState<string | null>(null);

  const [imageUrls, setImageUrls] = useState<{ [key: string]: string }>({});
  const [imageFiles, setImageFiles] = useState<File[]>([]);

 
  const [attributeMap, setAttributeMap] = useState<any>({});
  const [selectedAttributes, setSelectedAttributes] = useState<string[]>([]);
  const [rating, setRating] = useState("");

  const [form, setForm] = useState({
    title: "",
    description: "",
    productId: "",
    price: "",
    categoryId: "",
    image: "",
    stockCount: "",
    status: true,
  });

  const productRef = collection(db, "products");
  const categoryRef = collection(db, "categories");

  const loadProducts = async () => {
    const snapshot = await getDocs(productRef);

    const data = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...(doc.data() as any),
    })) as any[];

    setProducts(data);

    const urls: any = {};

    for (let p of data) {
      let path = "";

      if (Array.isArray(p?.images)) {
        path = p.images[0] ?? "";
      } else if (typeof p?.images === "string") {
        path = p.images;
      }

      if (!path) continue;

      try {
        if (path.startsWith("http")) {
          urls[p.id] = path;
        } else {
          const url = await getDownloadURL(ref(storage, path));
          urls[p.id] = url;
        }
      } catch {
        urls[p.id] = "https://via.placeholder.com/40";
      }
    }

    setImageUrls(urls);
  };

  const loadCategories = async () => {
    const snapshot = await getDocs(categoryRef);
    const data = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));
    setCategories(data);
  };

 
  const loadAttributes = async () => {
    const snapshot = await getDocs(productRef);

    const map: any = {};

    snapshot.forEach((doc) => {
      const data = doc.data();

      if (!data.attributes) return;

      data.attributes.forEach((attr: any) => {
        if (!map[attr.name]) {
          map[attr.name] = { type: attr.type || "text", values: [] };
        }

        if (Array.isArray(attr.values)) {
          map[attr.name].values.push(...attr.values);
          
          map[attr.name].values = Array.from(new Set(map[attr.name].values));
        }
      });
    });

    setAttributeMap(map);
  };

  useEffect(() => {
    loadProducts();
    loadCategories();
    loadAttributes();
  }, []);

  
  const uploadImages = async (): Promise<string[]> => {
    const urls: string[] = [];

    for (let file of imageFiles) {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("upload_preset", "product_upload");

      const res = await fetch(
        "https://api.cloudinary.com/v1_1/dtizyszoy/image/upload",
        { method: "POST", body: formData }
      );
      const data = await res.json();

      if (data.secure_url) urls.push(data.secure_url);
      else console.error("Upload failed:", data);
    }

    return urls;
  };

  
  const addProduct = async () => {
    if (!form.title || !form.price || !form.categoryId || !form.stockCount)
      return alert("Please fill all required fields");
    if (imageFiles.length === 0) return alert("Upload at least one image");

    const uploadedImages = await uploadImages();

    await addDoc(productRef, {
      title: form.title,
      description: form.description,
      productId: form.productId,
      price: Number(form.price),
      categoryId: form.categoryId,
      images: uploadedImages,
      stockCount: Number(form.stockCount),
      status: form.status,
      rating: Number(rating) || 0,
      
      attributes: selectedAttributes.map((name) => ({
        name,
        type: attributeMap[name].type || "text",
        values: attributeMap[name].values || [],
      })),
    });

    alert("Product added successfully!");
    resetForm();
    setImageFiles([]);
    setRating("");
    setSelectedAttributes([]);
    loadProducts();
    loadAttributes();
  };

 
  const updateProduct = async () => {
    if (!editingId) return;

    let uploadedImages: any = form.image;
    if (imageFiles.length > 0) uploadedImages = await uploadImages();

    await updateDoc(doc(db, "products", editingId), {
      ...form,
      rating: Number(rating) || 0,
      price: Number(form.price),
      stockCount: Number(form.stockCount),
      images: uploadedImages,
      attributes: selectedAttributes.map((name) => ({
        name,
        type: attributeMap[name].type || "text",
        values: attributeMap[name].values || [],
      })),
    });

    alert("Product updated successfully!");
    resetForm();
    setImageFiles([]);
    setRating("");
    setSelectedAttributes([]);
    loadProducts();
  };

  const deleteProduct = async (id: string) => {
    await deleteDoc(doc(db, "products", id));
    alert("Product deleted successfully!");
    loadProducts();
  };

  const editProduct = (p: any) => {
    setForm({
      title: p.title,
      description: p.description,
      productId: p.productId || "",
      price: p.price,
      categoryId: p.categoryId || "",
      image: p.images?.[0] || "",
      stockCount: p.stockCount,
      status: p.status,
    });
    setRating(p.rating || "");
    setEditingId(p.id);

    if (p.attributes?.length > 0) {
      setSelectedAttributes(p.attributes.map((a: any) => a.name));
     
      const updatedMap = { ...attributeMap };
      p.attributes.forEach((attr: any) => {
        if (!updatedMap[attr.name]) updatedMap[attr.name] = attr;
      });
      setAttributeMap(updatedMap);
    } else {
      setSelectedAttributes([]);
    }
  };

  const resetForm = () => {
    setEditingId(null);
    setForm({
      title: "",
      description: "",
      productId: "",
      price: "",
      categoryId: "",
      image: "",
      stockCount: "",
      status: true,
    });
    setRating("");
    setSelectedAttributes([]);
  };

  const getCategoryName = (id: string) => {
    const cat = categories.find((c) => c.categoryId === id);
    return cat?.name || cat?.categoryName || "N/A";
  };
  const downloadProductReport = () => {
  const doc = new jsPDF();

  // Product statistics
  const totalProducts = products.length;

  const activeProducts = products.filter(
    (product) => product.status === true
  ).length;

  const inactiveProducts = products.filter(
    (product) => product.status === false
  ).length;

  const outOfStock = products.filter(
    (product) => Number(product.stockCount || 0) <= 0
  ).length;

  const totalStock = products.reduce(
    (sum, product) => sum + Number(product.stockCount || 0),
    0
  );

  const totalProductValue = products.reduce(
    (sum, product) =>
      sum +
      Number(product.price || 0) *
        Number(product.stockCount || 0),
    0
  );

  const averagePrice =
    totalProducts > 0
      ? products.reduce(
          (sum, product) => sum + Number(product.price || 0),
          0
        ) / totalProducts
      : 0;

  // -----------------------------
  // Report Header
  // -----------------------------

  doc.setFontSize(20);
  doc.setFont("helvetica", "bold");
  doc.text("LuxeFlora Product Report", 14, 20);

  doc.setFontSize(10);
  doc.setFont("helvetica", "normal");

  doc.text(
    `Generated: ${new Date().toLocaleString()}`,
    14,
    28
  );

  // -----------------------------
  // Summary
  // -----------------------------

  doc.setFontSize(12);
  doc.setFont("helvetica", "bold");

  doc.text("Product Summary", 14, 40);

  doc.setFontSize(10);
  doc.setFont("helvetica", "normal");

  doc.text(
    `Total Products: ${totalProducts}`,
    14,
    48
  );

  doc.text(
    `Active Products: ${activeProducts}`,
    14,
    55
  );

  doc.text(
    `Inactive Products: ${inactiveProducts}`,
    14,
    62
  );

  doc.text(
    `Out of Stock: ${outOfStock}`,
    14,
    69
  );

  doc.text(
    `Total Stock Units: ${totalStock}`,
    110,
    48
  );

  doc.text(
    `Average Price: Rs. ${averagePrice.toFixed(2)}`,
    110,
    55
  );

  doc.text(
    `Total Stock Value: Rs. ${totalProductValue.toFixed(2)}`,
    110,
    62
  );

  // -----------------------------
  // Product Table
  // -----------------------------

  const tableData = products.map((product) => [
    product.productId || product.id,
    product.title || "-",
    getCategoryName(product.categoryId),
    `Rs. ${Number(product.price || 0).toFixed(2)}`,
    Number(product.stockCount || 0).toString(),
    Number(product.rating || 0).toFixed(1),
    product.status ? "Active" : "Inactive",
  ]);

  autoTable(doc, {
    startY: 78,

    head: [
      [
        "Product ID",
        "Product",
        "Category",
        "Price",
        "Stock",
        "Rating",
        "Status",
      ],
    ],

    body: tableData,

    styles: {
      fontSize: 8,
      cellPadding: 3,
    },

    headStyles: {
      fontStyle: "bold",
    },

    alternateRowStyles: {
      fillColor: [250, 245, 247],
    },

    columnStyles: {
      0: { cellWidth: 25 },
      1: { cellWidth: 40 },
      2: { cellWidth: 30 },
      3: { cellWidth: 25 },
      4: { cellWidth: 20 },
      5: { cellWidth: 20 },
      6: { cellWidth: 25 },
    },
  });

  // -----------------------------
  // Footer
  // -----------------------------

  const pageCount = doc.getNumberOfPages();

  for (let i = 1; i <= pageCount; i++) {
    doc.setPage(i);

    doc.setFontSize(8);
    doc.setFont("helvetica", "normal");

    doc.text(
      `LuxeFlora - Product Report | Page ${i} of ${pageCount}`,
      14,
      290
    );
  }

  // -----------------------------
  // Download
  // -----------------------------

  doc.save(
    `LuxeFlora-Product-Report-${new Date()
      .toISOString()
      .split("T")[0]}.pdf`
  );
};

  return (
    <div style={styles.container}>
  
      <div style={styles.card}>
        <h2>{editingId ? "Update Product" : "Add Product"}</h2>

        <div style={styles.row}>
          <input
            style={styles.input}
            placeholder="Title"
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
          />
          <input
            style={styles.input}
            placeholder="Price"
            value={form.price}
            onChange={(e) => setForm({ ...form, price: e.target.value })}
          />
        </div>

        <div style={styles.row}>
          <input
            style={{ ...styles.input, flex: 7 }}
            placeholder="Description"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
          <input
            style={{ ...styles.input, flex: 3 }}
            placeholder="Product ID"
            value={form.productId}
            onChange={(e) => setForm({ ...form, productId: e.target.value })}
          />
        </div>

        <div style={styles.row}>
          <select
            style={styles.input}
            value={form.categoryId}
            onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
          >
            <option value="">Select Category</option>
            {categories.map((c) => (
              <option key={c.id} value={c.categoryId}>
                {c.name}
              </option>
            ))}
          </select>

          <input
            style={styles.input}
            placeholder="Stock"
            value={form.stockCount}
            onChange={(e) => setForm({ ...form, stockCount: e.target.value })}
          />

          <input
            style={styles.input}
            placeholder="Rating"
            value={rating}
            onChange={(e) => setRating(e.target.value)}
          />
        </div>

        <div style={styles.row}>
          <input
            type="file"
            style={styles.input}
            onChange={(e) =>
              e.target.files &&
              setImageFiles([...imageFiles, e.target.files[0]])
            }
          />
          <input
            type="file"
            style={styles.input}
            onChange={(e) =>
              e.target.files &&
              setImageFiles([...imageFiles, e.target.files[0]])
            }
          />
          <input
            type="file"
            style={styles.input}
            onChange={(e) =>
              e.target.files &&
              setImageFiles([...imageFiles, e.target.files[0]])
            }
          />
        </div>

        
        <div style={styles.row}>
           <span style={{ fontWeight: "bold", marginRight: 10 }}>Select Attribute:</span>
  
          {Object.keys(attributeMap).map((attrName) => (
            <label key={attrName} style={{  marginRight: 15,   
        fontSize: 15,        
        display: "flex",
        alignItems: "center", }}>
              <input
                type="checkbox"
                checked={selectedAttributes.includes(attrName)}
                onChange={(e) => {
                  if (e.target.checked) {
                    setSelectedAttributes([...selectedAttributes, attrName]);
                  } else {
                    setSelectedAttributes(
                      selectedAttributes.filter((a) => a !== attrName)
                    );
                  }
                }}
                 style={{ marginRight: 6 }}
              />
              {attrName}
            </label>
          ))}
        </div>

        <button
          style={styles.button}
          onClick={editingId ? updateProduct : addProduct}
        >
          {editingId ? "Update Product" : "Add Product"}
        </button>
      </div>

    
      <div style={styles.card}>
  <div style={styles.productHeader}>
    <h2 style={{ margin: 0 }}>Products</h2>

    <button
      style={styles.reportButton}
      onClick={downloadProductReport}
    >
      Download Products Report
    </button>
  </div>

  <table style={styles.table}>
          <thead>
            <tr>
              <th>Image</th>
              <th>Title</th>
              <th>Product ID</th>
              <th>Price</th>
              <th>Category</th>
              <th>Stock</th>
              <th>Rating</th>
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>
            {products.map((p) => (
              <tr key={p.id}>
                <td>
                  <img
                    src={
                      imageUrls[p.id] ||
                      p.images?.[0] ||
                      "https://via.placeholder.com/40"
                    }
                    style={{ width: 40, height: 40, borderRadius: 8 }}
                  />
                </td>

                <td>{p.title}</td>
                <td>{p.productId || "-"}</td>
                <td>Rs. {p.price}</td>
                <td>{getCategoryName(p.categoryId)}</td>
                <td>{p.stockCount}</td>
                <td>{p.rating || 0}</td>
                <td>
                  <button
                    style={styles.buttonEdit}
                    onClick={() => editProduct(p)}
                  >
                    Edit
                  </button>

                  <button
                    style={styles.buttonDelete}
                    onClick={() => deleteProduct(p.id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}


const styles: any = {
  container: {
    marginLeft: "120px",
    marginTop: "70px",
    background: "#FFF7FA",
    padding: 20,
  },

  card: {
    background: "white",
    padding: 20,
    borderRadius: 16,
    marginBottom: 20,
    boxShadow: "0 8px 20px rgba(0,0,0,0.05)",
  },

  row: {
    display: "flex",
    gap: 10,
    marginBottom: 10,
  },
  buttonEdit: {
    padding: "6px 12px",
    marginRight: 6,
    borderRadius: 10,
    border: "none",
    cursor: "pointer",
    background: "linear-gradient(135deg, #4facfe, #00f2fe)",
    color: "white",
    fontWeight: "bold",
  },

  buttonDelete: {
    padding: "6px 12px",
    borderRadius: 10,
    border: "none",
    cursor: "pointer",
    background: "linear-gradient(135deg, #ff4b1f, #ff9068)",
    color: "white",
    fontWeight: "bold",
  },

  input: {
    flex: 1,
    padding: 12,
    borderRadius: 14,
    border: "1px solid #F3DDE1",
    outline: "none",
  },

  button: {
    width: "100%",
    padding: 12,
    background: "#8C4A5D",
    color: "white",
    border: "none",
    borderRadius: 14,
    cursor: "pointer",
    marginTop: 10,
  },

  table: {
    width: "100%",
    borderCollapse: "collapse",
    fontSize: 14,
  },
  productHeader: {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  marginBottom: 20,
},

reportButton: {
  padding: "10px 18px",
  borderRadius: 12,
  border: "none",
  background: "#8C4A5D",
  color: "white",
  cursor: "pointer",
  fontWeight: "bold",
  fontSize: 14,
},
};