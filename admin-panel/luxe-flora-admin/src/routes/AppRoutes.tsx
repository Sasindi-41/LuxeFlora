import { BrowserRouter, Routes, Route } from "react-router-dom";
import Login from "../pages/Login";
import Dashboard from "../pages/Dashboard";
import AdminLayout from "../layout/AdminLayout";
import Products from "../pages/Products";
import Orders from "../pages/Orders"; 
import Users from "../pages/Users"; 
import Admins from "../pages/Admins"; 
import Category from "../pages/Category"; 
import Attributes from "../pages/Attributes"; 
import Chats from "../pages/Chats"; 

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>

       
        <Route path="/" element={<Login />} />

       
        <Route
          path="/dashboard"
          element={
            <AdminLayout>
              <Dashboard />
            </AdminLayout>
          }
        />

        
        <Route
          path="/products"
          element={
            <AdminLayout>
              <Products />
            </AdminLayout>
          }
        />

         <Route
          path="/orders"
          element={
            <AdminLayout>
              <Orders />
            </AdminLayout>
          }
        />

           <Route
          path="/users"
          element={
            <AdminLayout>
              <Users />
            </AdminLayout>
          }
        />

        <Route
          path="/admins"
          element={
            <AdminLayout>
              <Admins />
            </AdminLayout>
          }
        />

         <Route
          path="/category"
          element={
            <AdminLayout>
              <Category />
            </AdminLayout>
          }
        />

          <Route
          path="/attributes"
          element={
            <AdminLayout>
              <Attributes />
            </AdminLayout>
          }
        />

          <Route
          path="/chats"
          element={
            <AdminLayout>
              <Chats />
            </AdminLayout>
          }
        />

      </Routes>
    </BrowserRouter>
  );
}