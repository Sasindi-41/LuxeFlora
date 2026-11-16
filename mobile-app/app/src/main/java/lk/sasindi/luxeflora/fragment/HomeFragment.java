package lk.sasindi.luxeflora.fragment;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import com.google.android.material.snackbar.Snackbar;
import android.os.Handler;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.activity.MainActivity;
import lk.sasindi.luxeflora.adapter.BannerAdapter;
import lk.sasindi.luxeflora.adapter.CategoryAdapter;
import lk.sasindi.luxeflora.adapter.SectionAdapter;
import lk.sasindi.luxeflora.model.Category;
import lk.sasindi.luxeflora.model.Product;
import lk.sasindi.luxeflora.util.SeenManager;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private List<Category> categoryList;
    private FirebaseFirestore db;

    private RecyclerView sectionRecycler;
    private SectionAdapter sectionAdapter;
    private List<Product> productList;
    private List<Product> filteredProductList;

    private ViewPager2 bannerSlider;
    private BannerAdapter bannerAdapter;
    private List<String> bannerList;
    private Handler sliderHandler = new Handler();
    private Runnable sliderRunnable;

    private FloatingActionButton fabMessages;
    private TextView messageBadge;

    private int unreadCount = 0;
    ImageView img1, img2, img3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        VideoView videoView = view.findViewById(R.id.flower_video);
        String cloudUrl = "https://res.cloudinary.com/dtizyszoy/video/upload/v1774605960/204802-925552197_medium_wmpmuc.mp4";
        videoView.setVideoURI(Uri.parse(cloudUrl));


        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoView.start();
        });


        img1 = view.findViewById(R.id.img1);
        img2 = view.findViewById(R.id.img2);
        img3 = view.findViewById(R.id.img3);

        Glide.with(this)
                .load("https://res.cloudinary.com/dtizyszoy/image/upload/v1774604826/62eb66d85c61911bdba0054bfcb54104_duiek3.jpg")
                .into(img1);

        Glide.with(this)
                .load("https://res.cloudinary.com/dtizyszoy/image/upload/v1774605446/vivi-mimi-TIR81z3-pMg-unsplash_wdmemw.jpg")
                .into(img2);

        Glide.with(this)
                .load("https://res.cloudinary.com/dtizyszoy/image/upload/v1774604844/profile1_vtmf2t.jpg")
                .into(img3);

        ImageView icon1, icon2, icon3, icon4;

        icon1 = view.findViewById(R.id.icon1);
        icon2 = view.findViewById(R.id.icon2);
        icon3 = view.findViewById(R.id.icon3);
        icon4 = view.findViewById(R.id.icon4);


        String url1 = "https://res.cloudinary.com/dtizyszoy/image/upload/v1774608710/flower_ujcgjw.png";
        String url2 = "https://res.cloudinary.com/dtizyszoy/image/upload/v1774608710/delivery-truck_at8eq9.png";
        String url3 = "https://res.cloudinary.com/dtizyszoy/image/upload/v1774608710/card_sgexdx.png";
        String url4 = "https://res.cloudinary.com/dtizyszoy/image/upload/v1774608710/flower_1_vmp9s8.png";

        Glide.with(this).load(url1).into(icon1);
        Glide.with(this).load(url2).into(icon2);
        Glide.with(this).load(url3).into(icon3);
        Glide.with(this).load(url4).into(icon4);

        recyclerView = view.findViewById(R.id.home_category_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        categoryList = new ArrayList<>();
        adapter = new CategoryAdapter(categoryList, category -> {
            Bundle bundle = new Bundle();
            bundle.putString("categoryId", category.getCategoryId());

            ListingFragment fragment = new ListingFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }, R.layout.home_item_category);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        db.collection("categories").get().addOnSuccessListener(snap -> {
            categoryList.clear();
            for (DocumentSnapshot doc : snap) {
                Category c = doc.toObject(Category.class);
                if (c != null) categoryList.add(c);
            }
            adapter.notifyDataSetChanged();
        });

        sectionRecycler = view.findViewById(R.id.home_section_recycler);
        sectionRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        productList = new ArrayList<>();
        filteredProductList = new ArrayList<>();
        sectionAdapter = new SectionAdapter(filteredProductList, product -> {

            Bundle bundle = new Bundle();
            bundle.putString("productId", product.getProductId());

            ProductDetailsFragment productDetailsFragment = new ProductDetailsFragment();
            productDetailsFragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, productDetailsFragment)
                    .addToBackStack(null)
                    .commit();
        });
        sectionRecycler.setAdapter(sectionAdapter);

        db.collection("products").get().addOnSuccessListener(snap -> {
            productList.clear();
            for (DocumentSnapshot doc : snap) {
                Product p = doc.toObject(Product.class);
                if (p != null) productList.add(p);
            }
            filteredProductList.clear();
            filteredProductList.addAll(productList);
            sectionAdapter.notifyDataSetChanged();
        });

        TextView viewAll = view.findViewById(R.id.home_section_more);
        viewAll.setOnClickListener(v -> {
            CategoryFragment categoryFragment = new CategoryFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, categoryFragment)
                    .addToBackStack(null) // Allows back button to return
                    .commit();
        });

       Button button = view.findViewById(R.id.shop_now_btn);
        button.setOnClickListener(v -> {
            CategoryFragment categoryFragment = new CategoryFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, categoryFragment)
                    .addToBackStack(null) // Allows back button to return
                    .commit();
        });



        bannerSlider = view.findViewById(R.id.home_banner_slider);
        bannerList = new ArrayList<>();
        bannerAdapter = new BannerAdapter(bannerList);
        bannerSlider.setAdapter(bannerAdapter);

        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (bannerList.size() == 0) return;

                int nextItem = bannerSlider.getCurrentItem() + 1;

                if (nextItem >= bannerList.size()) {
                    nextItem = 0; // loop back
                }

                bannerSlider.setCurrentItem(nextItem, true);
                sliderHandler.postDelayed(this, 3000);
            }
        };

        db.collection("banners").get().addOnSuccessListener(snap -> {
            bannerList.clear();
            for (DocumentSnapshot doc : snap) {
                String img = doc.getString("image");
                if (img != null) bannerList.add(img);
            }
            bannerAdapter.notifyDataSetChanged();
            sliderHandler.postDelayed(sliderRunnable, 3000);
        });

        fabMessages = view.findViewById(R.id.fab_messages);
        messageBadge = view.findViewById(R.id.message_badge);

        fabMessages.setOnClickListener(v -> {
            SeenManager.setLastSeen(requireContext(), System.currentTimeMillis());
            unreadCount = 0;
            updateBadge();

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MessageFragment())
                    .addToBackStack(null)
                    .commit();
        });

        listenForMessages();

        listenForOrderStatusUpdates();

        return view;
    }

    private void listenForMessages() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String chatId = FirebaseAuth.getInstance().getCurrentUser().getUid() + "_admin";

        FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null || snapshots == null) return;

                    long lastSeenTime = SeenManager.getLastSeen(requireContext());

                    int newCount = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Long time = doc.getLong("timestamp");
                        String senderId = doc.getString("senderId");

                        if (time == null) continue;

                        if ("admin".equals(senderId) && time > lastSeenTime) {
                            newCount++;
                        }
                    }

                    unreadCount = newCount;
                    updateBadge();
                });
    }

    private void updateBadge() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (unreadCount > 0) {
                messageBadge.setVisibility(View.VISIBLE);
                messageBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            } else {
                messageBadge.setVisibility(View.GONE);
            }
        });
    }

    private void showOrderNotification(String orderId, String status) {

        Context context = requireContext();
        String channelId = "order_updates_channel";

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Order Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground) // your app icon
                        .setContentTitle("Order Update")
                        .setContentText("Order " + orderId + " is now " + status)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
    public void filterProducts(String query) {
        filteredProductList.clear();

        if (query.isEmpty()) {
            filteredProductList.addAll(productList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Product p : productList) {
                if (p.getTitle() != null && p.getTitle().toLowerCase().contains(lowerQuery)) {
                    filteredProductList.add(p);
                }
            }
        }

        sectionAdapter.notifyDataSetChanged();
    }

    public void sortProducts(String option) {
        switch (option) {
            case "Name ASC":
                filteredProductList.sort((p1, p2) -> p1.getTitle().compareToIgnoreCase(p2.getTitle()));
                break;
            case "Name DSC":
                filteredProductList.sort((p1, p2) -> p2.getTitle().compareToIgnoreCase(p1.getTitle()));
                break;
            case "Price Low to High":
                filteredProductList.sort((p1, p2) -> Double.compare(p1.getPrice(), p2.getPrice()));
                break;
            case "Price High to Low":
                filteredProductList.sort((p1, p2) -> Double.compare(p2.getPrice(), p1.getPrice()));
                break;
        }
        sectionAdapter.notifyDataSetChanged();
    }

    private void listenForOrderStatusUpdates() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null || snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {

                        if (change.getType() == DocumentChange.Type.MODIFIED) {
                            String orderId = change.getDocument().getId();
                            String newStatus = change.getDocument().getString("status");

                            showOrderNotification(orderId, newStatus);
                        }
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }
}