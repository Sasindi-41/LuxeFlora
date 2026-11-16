import Sidebar from "../layout/Sidebar";


export default function AdminLayout({ children }: any) {
  return (
    <div>
      <Sidebar />
    
      <div>{children}</div>
    </div>
  );
}