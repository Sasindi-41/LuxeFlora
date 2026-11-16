package lk.sasindi.luxeflora.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.*;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.adapter.CartAdapter;
import lk.sasindi.luxeflora.databinding.FragmentCartBinding;
import lk.sasindi.luxeflora.model.CartItem;
import lk.sasindi.luxeflora.model.Product;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;

    private List<CartItem> cartItems;
    private List<CartItem> filteredCartItems;

    private CartAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {

            String uid = firebaseAuth.getCurrentUser().getUid();

            db.collection("users").document(uid).collection("cart")
                    .get()
                    .addOnSuccessListener(qds -> {
                        if (!qds.isEmpty()) {

                            cartItems = new ArrayList<>();
                            filteredCartItems = new ArrayList<>();

                            for (DocumentSnapshot ds : qds.getDocuments()) {
                                CartItem cartItem = ds.toObject(CartItem.class);
                                if (cartItem != null) {
                                    cartItem.setDocumentId(ds.getId());
                                    cartItems.add(cartItem);
                                }
                            }

                            filteredCartItems.addAll(cartItems);

                            binding.cartCartItems.setLayoutManager(new LinearLayoutManager(getContext()));

                            adapter = new CartAdapter(filteredCartItems);

                            adapter.setOnCartItemClickListener(cartItem -> {
                                Bundle bundle = new Bundle();
                                bundle.putString("productId", cartItem.getProductId());

                                ProductDetailsFragment fragment = new ProductDetailsFragment();
                                fragment.setArguments(bundle);

                                getParentFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_container, fragment)
                                        .addToBackStack(null)
                                        .commit();
                            });

                            adapter.setOnQuantityChangeListener(cartItem -> {
                                db.collection("users").document(uid)
                                        .collection("cart")
                                        .document(cartItem.getDocumentId())
                                        .update("quantity", cartItem.getQuantity());

                                updateTotal();
                            });

                            adapter.setOnRemoveListener(position -> {

                                String documentId = filteredCartItems.get(position).getDocumentId();

                                db.collection("users").document(uid)
                                        .collection("cart").document(documentId)
                                        .delete();

                                filteredCartItems.remove(position);
                                cartItems.removeIf(c -> c.getDocumentId().equals(documentId));

                                adapter.notifyDataSetChanged();
                                updateTotal();
                            });

                            binding.cartCartItems.setAdapter(adapter);
                            updateTotal();
                        }
                    });
        }

        View filterBtn = requireActivity().findViewById(R.id.btn_filter);
        filterBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), filterBtn);
            popup.getMenu().add("Name ASC");
            popup.getMenu().add("Name DSC");
            popup.getMenu().add("Price Low to High");
            popup.getMenu().add("Price High to Low");

            popup.setOnMenuItemClickListener(item -> {
                sortCart(item.getTitle().toString());
                return true;
            });

            popup.show();
        });

        binding.cartBtnProceed.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CheckoutFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    public void filterCart(String query) {

        if (cartItems == null || cartItems.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<String> productIds = new ArrayList<>();
        for (CartItem c : cartItems) {
            productIds.add(c.getProductId());
        }

        db.collection("products")
                .whereIn("productId", productIds)
                .get()
                .addOnSuccessListener(qds -> {

                    Map<String, Product> productMap = new HashMap<>();

                    for (DocumentSnapshot ds : qds.getDocuments()) {
                        Product p = ds.toObject(Product.class);
                        if (p != null) {
                            productMap.put(p.getProductId(), p);
                        }
                    }

                    filteredCartItems.clear();

                    if (query.isEmpty()) {
                        filteredCartItems.addAll(cartItems);
                    } else {
                        String lower = query.toLowerCase();

                        for (CartItem c : cartItems) {
                            Product p = productMap.get(c.getProductId());

                            if (p != null && p.getTitle() != null &&
                                    p.getTitle().toLowerCase().contains(lower)) {

                                filteredCartItems.add(c);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void sortCart(String option) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<String> ids = new ArrayList<>();
        for (CartItem c : filteredCartItems) ids.add(c.getProductId());

        db.collection("products").whereIn("productId", ids)
                .get()
                .addOnSuccessListener(qds -> {

                    Map<String, Product> map = new HashMap<>();
                    for (var doc : qds.getDocuments()) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) map.put(p.getProductId(), p);
                    }

                    switch (option) {
                        case "Name ASC":
                            filteredCartItems.sort((a, b) ->
                                    map.get(a.getProductId()).getTitle()
                                            .compareToIgnoreCase(map.get(b.getProductId()).getTitle()));
                            break;

                        case "Name DSC":
                            filteredCartItems.sort((a, b) ->
                                    map.get(b.getProductId()).getTitle()
                                            .compareToIgnoreCase(map.get(a.getProductId()).getTitle()));
                            break;

                        case "Price Low to High":
                            filteredCartItems.sort((a, b) ->
                                    Double.compare(
                                            map.get(a.getProductId()).getPrice(),
                                            map.get(b.getProductId()).getPrice()));
                            break;

                        case "Price High to Low":
                            filteredCartItems.sort((a, b) ->
                                    Double.compare(
                                            map.get(b.getProductId()).getPrice(),
                                            map.get(a.getProductId()).getPrice()));
                            break;
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void updateTotal() {
        if (cartItems == null || cartItems.isEmpty()) {
            binding.catTextTotal.setText(String.format(Locale.US, "LKR %,.2f", 0.00));
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<String> productIds = new ArrayList<>();
        cartItems.forEach(c -> productIds.add(c.getProductId()));

        db.collection("products")
                .whereIn("productId", productIds)
                .get()
                .addOnSuccessListener(qds -> {

                    Map<String, Product> map = new HashMap<>();

                    for (var doc : qds.getDocuments()) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) map.put(p.getProductId(), p);
                    }

                    double total = 0;
                    for (CartItem c : cartItems) {
                        Product p = map.get(c.getProductId());
                        if (p != null) {
                            total += p.getPrice() * c.getQuantity();
                        }
                    }

                    binding.catTextTotal.setText(String.format(Locale.US, "LKR %,.2f", total));
                });
    }
}