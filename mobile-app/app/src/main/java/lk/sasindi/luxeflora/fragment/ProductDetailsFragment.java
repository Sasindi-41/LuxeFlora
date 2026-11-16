package lk.sasindi.luxeflora.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.resourceinspection.annotation.Attribute;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.activity.SignInActivity;
import lk.sasindi.luxeflora.adapter.ProductSliderAdapter;
import lk.sasindi.luxeflora.adapter.SectionAdapter;
import lk.sasindi.luxeflora.databinding.FragmentProductDetailsBinding;
import lk.sasindi.luxeflora.model.CartItem;
import lk.sasindi.luxeflora.model.Product;

public class ProductDetailsFragment extends Fragment {

    private FragmentProductDetailsBinding binding;
    private String productId;
    private String categoryId;
    private int quantity = 1;
    private int avbQuantity;

    private boolean isWishlisted = false;
    private Map<String, ChipGroup> attributeGroups = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString("productId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().findViewById(R.id.bottom_navigation_view).setVisibility(View.GONE);

        getActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("products")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(qds -> {

                    if (!qds.isEmpty()) {

                        Product product = qds.getDocuments().get(0).toObject(Product.class);

                        categoryId = product.getCategoryId();
                        avbQuantity = product.getStockCount();

                        ProductSliderAdapter adapter =
                                new ProductSliderAdapter(product.getImages());

                        binding.productImageSlider.setAdapter(adapter);
                        binding.dotsIndicator.attachTo(binding.productImageSlider);

                        binding.productDetailsTitle.setText(product.getTitle());
                        binding.productDetailsRating.setRating((float) product.getRating());
                        binding.productDetailsPrice.setText("LKR " + product.getPrice());
                        binding.productDetailsQuantity.setText(String.valueOf(quantity));
                        binding.productDetailsDescription.setText(product.getDescription());
                        binding.productDetailsRatingValue.setText(String.valueOf(product.getRating()));

                        if (product.getStockCount() > 0) {

                            binding.productDetailsAvbQty.setText(String.valueOf(product.getStockCount()));
                            binding.productDetailsAvbQty.setTextColor(Color.parseColor("#777777"));

                            binding.productDetailsBtnAddCart.setEnabled(true);
                            binding.productDetailsBtnBuyNow.setEnabled(true);

                        } else {

                            binding.productDetailsAvbQty.setText("Out of Stock");
                            binding.productDetailsAvbQty.setTextColor(Color.RED);

                            binding.productDetailsBtnAddCart.setEnabled(false);
                            binding.productDetailsBtnAddCart.setAlpha(0.5f);

                            binding.productDetailsBtnBuyNow.setEnabled(false);
                            binding.productDetailsBtnBuyNow.setAlpha(0.5f);
                        }

                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        if (auth.getCurrentUser() != null) {

                            String uid = auth.getCurrentUser().getUid();

                            db.collection("users")
                                    .document(uid)
                                    .collection("wishlist")
                                    .document(productId)
                                    .get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc.exists()) {
                                            isWishlisted = true;
                                            binding.productDetailsWishlist.setImageResource(R.drawable.favorite_fill_24px);
                                        }
                                    });
                        }

                        List<Product.Attribute> attributes =
                                product.getAttributes() != null
                                        ? product.getAttributes()
                                        : new ArrayList<>();

                        for (Product.Attribute attribute : attributes) {
                            renderAttribute(attribute, binding.productDetailsAttributeContainer);
                        }

                        similarProduct();
                    }
                });

        binding.productDetailsBtnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.productDetailsQuantity.setText(String.valueOf(quantity));
            }
        });

        binding.productDetailsBtnPlus.setOnClickListener(v -> {
            if (avbQuantity > 0 && quantity < avbQuantity) {
                quantity++;
                binding.productDetailsQuantity.setText(String.valueOf(quantity));
            }
        });

        binding.productDetailsBtnAddCart.setOnClickListener(v -> {

            if (avbQuantity <= 0) {
                Toast.makeText(getContext(), "Out of stock", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();

            if (auth.getCurrentUser() == null) {
                startActivity(new Intent(getActivity(), SignInActivity.class));
                return;
            }

            List<CartItem.Attribute> attributes = getFinalSelections();
            CartItem cartItem = new CartItem(productId, quantity, attributes);

            String uid = auth.getCurrentUser().getUid();

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("cart")
                    .document(productId)
                    .set(cartItem)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(getContext(), "Item added to cart", Toast.LENGTH_SHORT).show()
                    );
        });

        binding.productDetailsBtnBuyNow.setOnClickListener(v -> {

            if (avbQuantity <= 0) {
                Toast.makeText(getContext(), "Out of stock", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();

            if (auth.getCurrentUser() == null) {
                startActivity(new Intent(getActivity(), SignInActivity.class));
                return;
            }

            Bundle bundle = new Bundle();
            bundle.putBoolean("isBuyNow", true);
            bundle.putString("productId", productId);
            bundle.putInt("quantity", quantity);

            CheckoutFragment fragment = new CheckoutFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.productDetailsWishlist.setOnClickListener(v -> {

            FirebaseAuth authClick = FirebaseAuth.getInstance();

            if (authClick.getCurrentUser() == null) {
                startActivity(new Intent(getActivity(), SignInActivity.class));
                return;
            }

            String uid = authClick.getCurrentUser().getUid();

            FirebaseFirestore db2 = FirebaseFirestore.getInstance();

            if (!isWishlisted) {

                Map<String, Object> map = new HashMap<>();
                map.put("productId", productId);

                db2.collection("users")
                        .document(uid)
                        .collection("wishlist")
                        .document(productId)
                        .set(map);

                isWishlisted = true;
                binding.productDetailsWishlist.setImageResource(R.drawable.favorite_fill_24px);

            } else {

                db2.collection("users")
                        .document(uid)
                        .collection("wishlist")
                        .document(productId)
                        .delete();

                isWishlisted = false;
                binding.productDetailsWishlist.setImageResource(R.drawable.favorite_24px);
            }
        });
    }

    private int darkenColor(int color) {
        float factor = 0.8f;

        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);

        return Color.rgb(r, g, b);
    }

    private void renderAttribute(Product.Attribute attribute, ViewGroup container) {

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 16, 0, 16);

        TextView label = new TextView(getContext());
        label.setText(attribute.getName());
        label.setTextSize(14);
        label.setTextColor(Color.parseColor("#666666"));
        label.setPadding(0, 0, 0, 8);

        row.addView(label);

        ChipGroup group = new ChipGroup(getContext());
        group.setSingleSelection(true);
        group.setSelectionRequired(true);


        for (String value : attribute.getValues()) {

            Chip chip = new Chip(getContext());
            chip.setCheckable(true);
            chip.setTag(value);

            if ("color".equals(attribute.getType())) {

                chip.setText("");
                int color = Color.parseColor(value);

                chip.setChipBackgroundColor(ColorStateList.valueOf(color));
                chip.setCheckedIconVisible(false);

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        chip.setChipBackgroundColor(
                                ColorStateList.valueOf(darkenColor(color))
                        );
                    } else {
                        chip.setChipBackgroundColor(
                                ColorStateList.valueOf(color)
                        );
                    }
                });

            } else {
                chip.setText(value);
            }

            group.addView(chip);
        }


        row.addView(group);
        container.addView(row);

        attributeGroups.put(attribute.getName(), group);
    }

    private List<CartItem.Attribute> getFinalSelections() {

        List<CartItem.Attribute> list = new ArrayList<>();

        for (Map.Entry<String, ChipGroup> entry : attributeGroups.entrySet()) {

            int id = entry.getValue().getCheckedChipId();

            if (id != -1) {
                Chip chip = getView().findViewById(id);
                list.add(new CartItem.Attribute(
                        entry.getKey(),
                        chip.getTag().toString()
                ));
            }
        }
        return list;
    }

    private void similarProduct() {
        if (categoryId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("products")
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener(qds -> {

                    List<Product> list = new ArrayList<>();

                    for (Product p : qds.toObjects(Product.class)) {
                        if (!p.getProductId().equals(productId)) {
                            list.add(p);
                        }
                    }

                    LinearLayoutManager lm = new LinearLayoutManager(
                            getContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                    );

                    binding.productDetailsTopSellSection.itemSectionContainer.setLayoutManager(lm);

                    SectionAdapter adapter = new SectionAdapter(list, product -> {

                        Bundle bundle = new Bundle();
                        bundle.putString("productId", product.getProductId());

                        ProductDetailsFragment fragment = new ProductDetailsFragment();
                        fragment.setArguments(bundle);

                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();
                    });

                    binding.productDetailsTopSellSection.itemSectionTitle.setText("Similar Products");
                    binding.productDetailsTopSellSection.itemSectionContainer.setAdapter(adapter);
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().findViewById(R.id.bottom_navigation_view).setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().findViewById(R.id.bottom_navigation_view).setVisibility(View.GONE);
    }
}