package lk.sasindi.luxeflora.fragment;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.adapter.ListingAdapter;
import lk.sasindi.luxeflora.databinding.FragmentListingBinding;
import lk.sasindi.luxeflora.model.Product;

public class ListingFragment extends Fragment {

    private FragmentListingBinding binding;
    private ListingAdapter adapter;
    private String categoryId;

    private List<Product> productList;
    private List<Product> filteredList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getString("categoryId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentListingBinding.inflate(inflater, container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerViewListing.setLayoutManager(new GridLayoutManager(getContext(),2));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        productList = new ArrayList<>();
        filteredList = new ArrayList<>();

        db.collection("products")
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener(ds -> {
                    if (!ds.isEmpty()){

                        productList.clear();
                        productList.addAll(ds.toObjects(Product.class));

                        filteredList.clear();
                        filteredList.addAll(productList);

                        adapter = new ListingAdapter(filteredList, product -> {


                            if (product.getStockCount() <= 0) {
                                Toast.makeText(getContext(), "Out of stock", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Bundle bundle = new Bundle();
                            bundle.putString("productId", product.getProductId());

                            ProductDetailsFragment productDetailsFragment = new ProductDetailsFragment();
                            productDetailsFragment.setArguments(bundle);

                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, productDetailsFragment)
                                    .addToBackStack(null)
                                    .commit();

                        });

                        binding.recyclerViewListing.setAdapter(adapter);
                    }
                });

        View filterBtn = requireActivity().findViewById(R.id.btn_filter);
        filterBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), filterBtn);
            popup.getMenu().add("Name ASC");
            popup.getMenu().add("Name DSC");
            popup.getMenu().add("Price Low to High");
            popup.getMenu().add("Price High to Low");

            popup.setOnMenuItemClickListener(item -> {
                sortProducts(item.getTitle().toString());
                return true;
            });

            popup.show();
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });
    }

    public void filterProducts(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(productList);
        } else {
            String lower = query.toLowerCase();
            for (Product p : productList) {
                if (p.getTitle() != null && p.getTitle().toLowerCase().contains(lower)) {
                    filteredList.add(p);
                }
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }


    public void sortProducts(String option) {

        if (filteredList == null || filteredList.isEmpty()) return;

        switch (option) {
            case "Name ASC":
                filteredList.sort((p1, p2) -> p1.getTitle().compareToIgnoreCase(p2.getTitle()));
                break;

            case "Name DSC":
                filteredList.sort((p1, p2) -> p2.getTitle().compareToIgnoreCase(p1.getTitle()));
                break;

            case "Price Low to High":
                filteredList.sort((p1, p2) -> Double.compare(p1.getPrice(), p2.getPrice()));
                break;

            case "Price High to Low":
                filteredList.sort((p1, p2) -> Double.compare(p2.getPrice(), p1.getPrice()));
                break;
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }
}