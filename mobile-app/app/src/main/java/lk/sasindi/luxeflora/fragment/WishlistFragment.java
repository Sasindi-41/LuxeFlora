package lk.sasindi.luxeflora.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.adapter.WishlistAdapter;
import lk.sasindi.luxeflora.databinding.FragmentWishlistBinding;
import lk.sasindi.luxeflora.model.Product;
import lk.sasindi.luxeflora.util.WishlistManager;

import lk.sasindi.luxeflora.util.WishlistLocalManager;

public class WishlistFragment extends Fragment {

    private FragmentWishlistBinding binding;
    private WishlistManager wishlistManager;
    private FirebaseFirestore db;

    private List<Product> wishlistProducts;
    private List<Product> filteredList;
    private WishlistAdapter adapter;

    private WishlistLocalManager localManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentWishlistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        wishlistManager = new WishlistManager(getContext());
        db = FirebaseFirestore.getInstance();

        localManager = new WishlistLocalManager(getContext());

        loadWishlist();

        View filterBtn = requireActivity().findViewById(R.id.btn_filter);
        filterBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), filterBtn);
            popup.getMenu().add("Name ASC");
            popup.getMenu().add("Name DSC");
            popup.getMenu().add("Price Low to High");
            popup.getMenu().add("Price High to Low");

            popup.setOnMenuItemClickListener(item -> {
                sortWishlist(item.getTitle().toString());
                return true;
            });

            popup.show();
        });
    }

    private void loadWishlist() {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        wishlistProducts = new ArrayList<>();
        filteredList = new ArrayList<>();

        List<Product> offlineList = localManager.getAll();

        if (!offlineList.isEmpty()) {
            wishlistProducts.addAll(offlineList);
            filteredList.addAll(offlineList);

            adapter = new WishlistAdapter(filteredList);
            binding.wishlistItems.setLayoutManager(
                    new LinearLayoutManager(getContext())
            );
            binding.wishlistItems.setAdapter(adapter);

            setupAdapterListeners(uid);
        }

        db.collection("users")
                .document(uid)
                .collection("wishlist")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<String> productIds = new ArrayList<>();

                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        String productId = doc.getString("productId");
                        if (productId != null) {
                            productIds.add(productId);
                        }
                    }

                    if (productIds.isEmpty()) return;

                    db.collection("products")
                            .whereIn("productId", productIds)
                            .get()
                            .addOnSuccessListener(qds -> {

                                wishlistProducts.clear();
                                filteredList.clear();

                                for (var doc : qds.getDocuments()) {
                                    Product product = doc.toObject(Product.class);
                                    if (product != null) {
                                        wishlistProducts.add(product);
                                    }
                                }

                                filteredList.addAll(wishlistProducts);

                                adapter = new WishlistAdapter(filteredList);

                                binding.wishlistItems.setLayoutManager(
                                        new LinearLayoutManager(getContext())
                                );

                                binding.wishlistItems.setAdapter(adapter);

                                setupAdapterListeners(uid);

                                localManager.clear();

                                for (Product product : wishlistProducts) {
                                    localManager.insert(product);
                                }
                            });
                });
    }

    private void setupAdapterListeners(String uid) {

        adapter.setOnItemClickListener(product -> {
            Bundle bundle = new Bundle();
            bundle.putString("productId", product.getProductId());

            ProductDetailsFragment fragment = new ProductDetailsFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        adapter.setOnRemoveListener(position -> {

            Product product = filteredList.get(position);

            db.collection("users")
                    .document(uid)
                    .collection("wishlist")
                    .document(product.getProductId())
                    .delete();

            localManager.delete(product.getProductId());

            wishlistProducts.remove(product);
            filteredList.remove(position);

            adapter.notifyDataSetChanged();
        });
    }

    public void filterWishlist(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(wishlistProducts);
        } else {
            String lower = query.toLowerCase();
            for (Product p : wishlistProducts) {
                if (p.getTitle() != null &&
                        p.getTitle().toLowerCase().contains(lower)) {
                    filteredList.add(p);
                }
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void sortWishlist(String option) {

        switch (option) {
            case "Name ASC":
                filteredList.sort((a, b) ->
                        a.getTitle().compareToIgnoreCase(b.getTitle()));
                break;

            case "Name DSC":
                filteredList.sort((a, b) ->
                        b.getTitle().compareToIgnoreCase(a.getTitle()));
                break;

            case "Price Low to High":
                filteredList.sort((a, b) ->
                        Double.compare(a.getPrice(), b.getPrice()));
                break;

            case "Price High to Low":
                filteredList.sort((a, b) ->
                        Double.compare(b.getPrice(), a.getPrice()));
                break;
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }
}