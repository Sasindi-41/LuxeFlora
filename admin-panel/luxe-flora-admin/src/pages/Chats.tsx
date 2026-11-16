import { collection, getDocs, doc, getDoc } from "firebase/firestore";
import { db } from "../firebase/firebaseConfig";
import { useEffect, useState } from "react";
import ChatRoom from "./ChatRoom";

type ChatThread = {
  id: string;
  userName: string;
  profilePic: string;
};

export default function Chats() {
  const [chats, setChats] = useState<ChatThread[]>([]);
  const [selectedChat, setSelectedChat] = useState("");

  useEffect(() => {
    const fetchChats = async () => {
      const snap = await getDocs(collection(db, "chats"));

      const result: ChatThread[] = await Promise.all(
        snap.docs.map(async (chatDoc) => {
          const chatId = chatDoc.id;

          
          const userId = chatId.replace("_admin", "");

          let userName = "Unknown User";
          let profilePic = "";

          try {
            const userRef = doc(db, "users", userId);
            const userSnap = await getDoc(userRef);

            if (userSnap.exists()) {
              const userData = userSnap.data();
              userName = userData.name || "Unknown User";
              profilePic = userData.profilePicUrl || "";
            }
          } catch (error) {
            console.log("Error fetching user:", error);
          }

          return {
            id: chatId,
            userName,
            profilePic,
          };
        })
      );

      setChats(result);
    };

    fetchChats();
  }, []);

  return (
    <div style={{ display: "flex", height: "100vh", marginLeft: "260px", marginTop: "70px" }}>
      
   
      <div style={{ width: "280px", borderRight: "1px solid #ddd", background: "#fafafa" }}>
        <h3 style={{ padding: 15 }}>Chat List</h3>

        {chats.map((chat) => (
          <div
            key={chat.id}
            onClick={() => setSelectedChat(chat.id)}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "10px",
              padding: 15,
              cursor: "pointer",
              borderBottom: "1px solid #eee",
              background: selectedChat === chat.id ? "#eee" : "white",
            }}
          >
         
            <img
              src={chat.profilePic || "https://via.placeholder.com/40"}
              alt="profile"
              style={{
                width: 40,
                height: 40,
                borderRadius: "50%",
                objectFit: "cover",
              }}
            />

          
            <span>{chat.userName}</span>
          </div>
        ))}
      </div>

      
      <div style={{ flex: 1 }}>
        {selectedChat ? (
          <ChatRoom chatId={selectedChat} />
        ) : (
          <div style={{ padding: 20, color: "gray" }}>
            Select a chat
          </div>
        )}
      </div>

    </div>
  );
}