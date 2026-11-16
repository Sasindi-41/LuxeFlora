package lk.sasindi.luxeflora.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.adapter.OrdersAdapter;
import lk.sasindi.luxeflora.model.Order;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrdersAdapter adapter;
    private List<Order> orderList;
    private FirebaseFirestore db;

    private MaterialButton tabAll, tabPending, tabShipped, tabDelivered;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        recyclerView = view.findViewById(R.id.ordersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        orderList = new ArrayList<>();
        adapter = new OrdersAdapter(getContext(), orderList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        tabAll = view.findViewById(R.id.tabAll);
        tabPending = view.findViewById(R.id.tabPending);
        tabShipped = view.findViewById(R.id.tabProgress);
        tabDelivered = view.findViewById(R.id.tabCompleted);

        tabAll.setOnClickListener(v -> loadOrders("All"));
        tabPending.setOnClickListener(v -> loadOrders("Pending"));
        tabShipped.setOnClickListener(v -> loadOrders("Shipped"));
        tabDelivered.setOnClickListener(v -> loadOrders("Delivered"));

        loadOrders("All");

        return view;
    }

    private void loadOrders(String status) {

        db.collection("orders")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    orderList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Order order = doc.toObject(Order.class);

                        if (order != null) {
                            if (status.equals("All") || order.getStatus().equalsIgnoreCase(status)) {
                                orderList.add(order);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    highlightSelectedTab(status);
                });
    }

    private void highlightSelectedTab(String status) {
        MaterialButton[] tabs = {tabAll, tabPending, tabShipped, tabDelivered};
        for (MaterialButton tab : tabs) {
            tab.setBackgroundTintList(getResources().getColorStateList(R.color.md_theme_secondaryContainer)); // default color
            tab.setTextColor(getResources().getColor(R.color.md_theme_primary));
        }

        switch (status) {
            case "All":
                tabAll.setBackgroundTintList(getResources().getColorStateList(R.color.md_theme_primary));
                tabAll.setTextColor(getResources().getColor(R.color.md_theme_secondaryContainer));
                break;
            case "Pending":
                tabPending.setBackgroundTintList(getResources().getColorStateList(R.color.md_theme_primary));
                tabPending.setTextColor(getResources().getColor(R.color.md_theme_secondaryContainer));
                break;
            case "Shipped":
                tabShipped.setBackgroundTintList(getResources().getColorStateList(R.color.md_theme_primary));
                tabShipped.setTextColor(getResources().getColor(R.color.md_theme_secondaryContainer));
                break;
            case "Delivered":
                tabDelivered.setBackgroundTintList(getResources().getColorStateList(R.color.md_theme_primary));
                tabDelivered.setTextColor(getResources().getColor(R.color.md_theme_secondaryContainer));
                break;
        }
    }
}