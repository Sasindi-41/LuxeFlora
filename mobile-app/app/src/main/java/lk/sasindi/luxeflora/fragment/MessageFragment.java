package lk.sasindi.luxeflora.fragment;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.adapter.MessageAdapter;
import lk.sasindi.luxeflora.model.Message;

public class MessageFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText messageInput;
    private Button sendBtn;

    private FirebaseFirestore db;

    private List<Message> messageList = new ArrayList<>();
    private MessageAdapter adapter;

    private String chatId;
    private String currentUserId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else {
                    requireActivity().finish();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_message, container, false);

        recyclerView = view.findViewById(R.id.messageRecycler);
        messageInput = view.findViewById(R.id.messageInput);
        sendBtn = view.findViewById(R.id.sendBtn);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("CHAT", "User NOT logged in!");
            return view;
        }

        currentUserId = user.getUid();
        Log.d("CHAT", "Current UID: " + currentUserId);

        chatId = currentUserId + "_admin";

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        Log.e("CHAT", "User document NOT found!");
                        return;
                    }

                    String userName = userDoc.getString("name");
                    String profilePic = userDoc.getString("profilePicUrl");

                    Log.d("CHAT", "User Name: " + userName);

                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("userId", currentUserId);
                    chatData.put("userName", userName);
                    chatData.put("profilePic", profilePic);
                    chatData.put("createdAt", System.currentTimeMillis());

                    db.collection("chats")
                            .document(chatId)
                            .set(chatData);
                });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MessageAdapter(messageList, currentUserId);
        recyclerView.setAdapter(adapter);

        sendBtn.setOnClickListener(v -> sendMessage());

        loadMessages();

        return view;
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", currentUserId);
        msg.put("receiverId", "admin");
        msg.put("text", text);
        msg.put("timestamp", System.currentTimeMillis());

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(msg);

        messageInput.setText("");
    }

    private void loadMessages() {
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    messageList.clear();
                    if (value != null) {
                        for (var doc : value) {
                            Message msg = doc.toObject(Message.class);
                            messageList.add(msg);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}