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
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";

export default function Orders() {
  const [orders, setOrders] = useState<any[]>([]);
  const [imageCache, setImageCache] = useState<any>({});

  const orderRef = collection(db, "orders");

  const loadOrders = async () => {
    const snapshot = await getDocs(orderRef);
    const data = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...(doc.data() as any),
    })) as any[];

    setOrders(data);

    const cache: any = {};

    for (let order of data) {
      for (const item of (order.orderItems ?? []) as any[]) {
        const key = item.productId;

        let path = item.productImage;

        if (!path) continue;

        try {
          if (path.startsWith("http")) {
            cache[key] = path;
          } else {
            const url = await getDownloadURL(ref(storage, path));
            cache[key] = url;
          }
        } catch {
          cache[key] = "https://via.placeholder.com/40";
        }
      }
    }

    setImageCache(cache);
  };

  useEffect(() => {
    loadOrders();
  }, []);

  const updateStatus = async (id: string, status: string) => {
    await updateDoc(doc(db, "orders", id), {
      status,
    });

    loadOrders();
  };
  const downloadReport = () => {
  const doc = new jsPDF();

  const totalOrders = orders.length;

  const totalRevenue = orders.reduce(
    (sum, order) => sum + Number(order.totalAmount || 0),
    0
  );

  const totalItems = orders.reduce(
    (sum, order) =>
      sum +
      (order.orderItems?.reduce(
        (itemSum: number, item: any) =>
          itemSum + Number(item.quantity || 0),
        0
      ) || 0),
    0
  );

  const pendingOrders = orders.filter(
    (order) => order.status === "Pending"
  ).length;

  const shippedOrders = orders.filter(
    (order) => order.status === "Shipped"
  ).length;

  const deliveredOrders = orders.filter(
    (order) => order.status === "Delivered"
  ).length;

 
  doc.setFontSize(20);
  doc.setFont("helvetica", "bold");
  doc.text("LuxeFlora Orders Report", 14, 20);


  doc.setFontSize(10);
  doc.setFont("helvetica", "normal");

  doc.text(
    `Generated: ${new Date().toLocaleString()}`,
    14,
    28
  );


  doc.setFontSize(12);
  doc.setFont("helvetica", "bold");
  doc.text("Order Summary", 14, 40);

  doc.setFontSize(10);
  doc.setFont("helvetica", "normal");

  doc.text(`Total Orders: ${totalOrders}`, 14, 48);
  doc.text(`Total Items Sold: ${totalItems}`, 14, 55);
  doc.text(`Total Revenue: Rs. ${totalRevenue.toFixed(2)}`, 14, 62);

  doc.text(`Pending: ${pendingOrders}`, 110, 48);
  doc.text(`Shipped: ${shippedOrders}`, 110, 55);
  doc.text(`Delivered: ${deliveredOrders}`, 110, 62);

 
  const tableData = orders.map((order) => {
    const quantity =
      order.orderItems?.reduce(
        (sum: number, item: any) =>
          sum + Number(item.quantity || 0),
        0
      ) || 0;

    const orderDate = order.orderDate?.seconds
      ? new Date(
          order.orderDate.seconds * 1000
        ).toLocaleDateString()
      : "-";

    return [
      order.orderId || order.id,
      order.shippingAddress?.name || "-",
      orderDate,
      quantity.toString(),
      `Rs. ${Number(order.totalAmount || 0).toFixed(2)}`,
      order.status || "-",
    ];
  });

  autoTable(doc, {
    startY: 72,
    head: [
      [
        "Order ID",
        "Customer",
        "Date",
        "Qty",
        "Total",
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
      fillColor: [245, 245, 245],
    },
  });

  
  const pageCount = doc.getNumberOfPages();

  for (let i = 1; i <= pageCount; i++) {
    doc.setPage(i);

    doc.setFontSize(8);
    doc.setFont("helvetica", "normal");

    doc.text(
      `LuxeFlora - Orders Report | Page ${i} of ${pageCount}`,
      14,
      290
    );
  }

  
  doc.save(
    `LuxeFlora-Orders-Report-${new Date()
      .toISOString()
      .split("T")[0]}.pdf`
  );
};

  return (
    <div style={styles.container}>
   <div style={styles.header}>
  <h2 style={styles.title}>Orders Management</h2>

  <button
    onClick={downloadReport}
    style={styles.reportButton}
  >
    Download Orders Report
  </button>
</div>

<div style={styles.card}>

        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}>Order ID</th>
              <th style={styles.th}>Customer</th>
              <th style={styles.th}>Total</th>
              <th style={styles.th}>Quantity</th>
              <th style={styles.th}>Order Date</th>
              <th style={styles.th}>Status</th>
              <th style={styles.th}>Items</th>
              <th style={styles.th}>Action</th>
            </tr>
          </thead>

          <tbody>
            {orders.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: "center", padding: 20 }}>
                  No orders found
                </td>
              </tr>
            ) : (
              orders.map((order) => (
                <tr key={order.id} style={styles.tr}>
                  <td style={styles.td}>{order.orderId}</td>

                  <td style={styles.td}>
                    <div>
                      <b>{order.shippingAddress?.name}</b>
                      <br />
                      {order.shippingAddress?.email}
                    </div>
                  </td>

                  <td style={styles.td}>Rs. {order.totalAmount}</td>

                  <td style={styles.td}>
                    {order.orderItems?.reduce(
                      (sum: number, item: any) => sum + item.quantity,
                      0
                    )}
                  </td>

                  <td style={styles.td}>
                    {order.orderDate?.seconds
                      ? new Date(order.orderDate.seconds * 1000).toLocaleString()
                      : "-"}
                  </td>

                  <td style={styles.td}>
                    <span
                      style={{
                        ...styles.status,
                        background:
                          order.status === "Shipped"
                            ? "#2A9D8F"
                            : order.status === "Delivered"
                            ? "#6C757D"
                            : "#F4A261",
                      }}
                    >
                      {order.status}
                    </span>
                  </td>

                  <td style={styles.td}>
                    {order.orderItems?.map((item: any, index: number) => (
                      <div key={index} style={styles.itemBox}>
                        <img
                          src={
                            imageCache[item.productId] ||
                            item.productImage ||
                            "https://via.placeholder.com/40"
                          }
                          style={styles.img}
                        />

                        <div>
                          <div>{item.productName}</div>
                          <small>
                            {item.quantity} × Rs. {item.unitPrice}
                          </small>

                          {item.attributes?.map((a: any, i: number) => (
                            <div key={i} style={{ fontSize: 12 }}>
                              {a.name}: {a.value}
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </td>

                  <td style={styles.td}>
                    <select
                      value={order.status}
                      onChange={(e) =>
                        updateStatus(order.id, e.target.value)
                      }
                      style={styles.select}
                    >
                      <option value="Pending">Pending</option>
                      <option value="Shipped">Shipped</option>
                      <option value="Delivered">Delivered</option>
                    </select>
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
  margin: 0,
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
    padding: "12px",
    verticalAlign: "top",
  },

  img: {
    width: 35,
    height: 35,
    borderRadius: 8,
    marginRight: 10,
  },

  itemBox: {
    display: "flex",
    alignItems: "center",
    marginBottom: 5,
    fontSize: 12,
    gap: 10,
  },

  status: {
    padding: "5px 10px",
    borderRadius: 12,
    color: "white",
    display: "inline-block",
  },

  select: {
    padding: "6px 10px",
    borderRadius: 10,
    border: "1px solid #ddd",
    background: "white",
    cursor: "pointer",
  },
  header: {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  marginBottom: "30px",
},

reportButton: {
  background: "#8C4A5D",
  color: "#fff",
  border: "none",
  padding: "10px 18px",
  borderRadius: 8,
  cursor: "pointer",
  fontSize: 14,
  fontWeight: 600,
},
};