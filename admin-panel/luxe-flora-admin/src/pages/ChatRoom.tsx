import { collection, addDoc, query, orderBy, onSnapshot } from "firebase/firestore";
import { db } from "../firebase/firebaseConfig";
import { useEffect, useState } from "react";

type Message = {
  senderId: string;
  text: string;
};

export default function ChatRoom({ chatId }: { chatId: string }) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [text, setText] = useState("");

  const currentUserId = "admin";

  useEffect(() => {
    const q = query(
      collection(db, "chats", chatId, "messages"),
      orderBy("timestamp")
    );

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const msgs: Message[] = snapshot.docs.map(doc => doc.data() as Message);
      setMessages(msgs);
    });

    return () => unsubscribe();
  }, [chatId]);

  const sendMessage = async () => {
    if (!text.trim()) return;

    await addDoc(collection(db, "chats", chatId, "messages"), {
      senderId: "admin",
      receiverId: chatId.replace("_admin", ""),
      text,
      timestamp: Date.now(),
    });

    setText("");
  };

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        height: "90vh", 
        border: "1px solid #ddd",
        borderRadius: "10px",
        margin: "20px",
        overflow: "hidden",
        background: "#fff"
      }}
    >

     

      <div
        style={{
          flex: 1,
          padding: "10px",
          overflowY: "auto",
          background: "#f5f5f5"
        }}
      >
        {messages.map((msg, index) => (
          <div
            key={index}
            style={{
              textAlign: msg.senderId === currentUserId ? "right" : "left",
              marginBottom: "10px"
            }}
          >
            <span
              style={{
                display: "inline-block",
                padding: "8px 12px",
                borderRadius: "10px",
                background:
                  msg.senderId === currentUserId ? "#dcf8c6" : "#fff",
                border: "1px solid #ddd"
              }}
            >
              {msg.text}
            </span>
          </div>
        ))}
      </div>

    
      <div style={{
        display: "flex",
        borderTop: "1px solid #eee"
      }}>
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          style={{
            flex: 1,
            padding: "10px",
            border: "none",
            outline: "none"
          }}
          placeholder="Type a message..."
        />

        <button
          onClick={sendMessage}
          style={{
            padding: "10px 20px",
            border: "none",
            background: "#4CAF50",
            color: "white",
            cursor: "pointer"
          }}
        >
          Send
        </button>
      </div>

    </div>
  );
}