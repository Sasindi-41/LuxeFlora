import { useEffect, useState } from "react";
import { collection, getDocs } from "firebase/firestore";
import { db } from "../firebase/firebaseConfig";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
} from "recharts";

export default function Dashboard() {
  const [orders, setOrders] = useState<any[]>([]);
  const [products, setProducts] = useState<any[]>([]);
  const [users, setUsers] = useState<any[]>([]);

  useEffect(() => {
    const load = async () => {
      const ordersSnap = await getDocs(collection(db, "orders"));
      const productsSnap = await getDocs(collection(db, "products"));
      const usersSnap = await getDocs(collection(db, "users"));

      setOrders(ordersSnap.docs.map((d) => ({ id: d.id, ...d.data() })));
      setProducts(productsSnap.docs.map((d) => ({ id: d.id, ...d.data() })));
      setUsers(usersSnap.docs.map((d) => ({ id: d.id, ...d.data() })));
    };

    load();
  }, []);

  
  const getMonth = (ts: any) => {
    if (!ts) return "Unknown";
    const date = ts?.seconds ? new Date(ts.seconds * 1000) : new Date(ts);
    return date.toLocaleString("default", { month: "short", year: "numeric" });
  };

 
  const totalOrders = orders.length;

  const totalRevenue = orders.reduce(
    (sum, o) => sum + (o.totalAmount || 0),
    0
  );

  const totalCustomers = new Set(
    orders.map((o) => o.shippingAddress?.email).filter(Boolean)
  ).size;

  const activeUsers = users.filter((u) => u.status === "active").length;


  const revenueMap: Record<string, number> = {};

  orders.forEach((o) => {
    const month = getMonth(o.orderDate);
    revenueMap[month] =
      (revenueMap[month] || 0) + (o.totalAmount || 0);
  });

  const revenueData = Object.keys(revenueMap).map((m) => ({
    month: m,
    revenue: revenueMap[m],
  }));

  
  const ordersMap: Record<string, number> = {};

  orders.forEach((o) => {
    const month = getMonth(o.orderDate);
    ordersMap[month] = (ordersMap[month] || 0) + 1;
  });

  const ordersData = Object.keys(ordersMap).map((m) => ({
    month: m,
    orders: ordersMap[m],
  }));

 
  const productSales: Record<string, number> = {};

  orders.forEach((order) => {
    order.orderItems?.forEach((item: any) => {
      const name = item.productName || item.productId;
      productSales[name] =
        (productSales[name] || 0) + (item.quantity || 0);
    });
  });

  const topProducts = Object.entries(productSales)
    .map(([name, qty]) => ({ name, qty }))
    .sort((a, b) => b.qty - a.qty)
    .slice(0, 5);

  
  const categoryMap: Record<string, number> = {};

  products.forEach((p) => {
    const cat = p.categoryId || "Unknown";
    categoryMap[cat] = (categoryMap[cat] || 0) + 1;
  });

  const categoryData = Object.keys(categoryMap).map((c) => ({
    name: c,
    value: categoryMap[c],
  }));

  const COLORS = ["#8C4A5D", "#C97B8B", "#FFB6C1", "#FF7070", "#FADADD"];

  return (
    <div style={styles.container}>
     
      <div style={styles.grid}>
        <div style={styles.card}>
          <h4>Total Orders</h4>
          <h2>{totalOrders}</h2>
        </div>

        <div style={styles.card}>
          <h4>Total Revenue</h4>
          <h2>Rs. {totalRevenue}</h2>
        </div>

        <div style={styles.card}>
          <h4>Customers</h4>
          <h2>{totalCustomers}</h2>
        </div>

        <div style={styles.card}>
          <h4>Active Users</h4>
          <h2>{activeUsers}</h2>
        </div>
      </div>

      
      <div style={styles.chartGrid}>
     
        <div style={styles.chartCard}>
          <h3>Revenue Trend</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={revenueData}>
              <XAxis dataKey="month" />
              <YAxis />
              <Tooltip />
              <Line
                type="monotone"
                dataKey="revenue"
                stroke="#8C4A5D"
                strokeWidth={3}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

      
        <div style={styles.chartCard}>
          <h3>Orders Trend</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={ordersData}>
              <XAxis dataKey="month" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="orders" fill="#C97B8B" />
            </BarChart>
          </ResponsiveContainer>
        </div>

       
        <div style={styles.chartCard}>
          <h3>Category Distribution</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={categoryData}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                outerRadius={90}
                label
              >
                {categoryData.map((_, i) => (
                  <Cell key={i} fill={COLORS[i % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>

    
        <div style={styles.chartCard}>
          <h3>Top Products</h3>

          {topProducts.length === 0 ? (
            <p>No sales data yet</p>
          ) : (
            topProducts.map((p, i) => (
              <div key={i} style={styles.productRow}>
                <span>{p.name}</span>
                <b>{p.qty} sold</b>
              </div>
            ))
          )}
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
    background: "#FFF7FA",
    minHeight: "100vh",
  },

  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(4, 1fr)",
    gap: 20,
  },

  card: {
    background: "white",
    padding: 20,
    borderRadius: 16,
    boxShadow: "0 8px 20px rgba(140,74,93,0.12)",
    textAlign: "center",
    color: "#8C4A5D",
  },

  chartGrid: {
    marginTop: 20,
    display: "grid",
    gridTemplateColumns: "repeat(2, 1fr)",
    gap: 20,
  },

  chartCard: {
    background: "white",
    padding: 20,
    borderRadius: 16,
    boxShadow: "0 8px 20px rgba(140,74,93,0.1)",
  },

  productRow: {
    display: "flex",
    justifyContent: "space-between",
    padding: "8px 0",
    borderBottom: "1px solid #eee",
  },
};