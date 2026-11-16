package lk.sasindi.luxeflora.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;
import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.databinding.FragmentCheckoutBinding;
import lk.sasindi.luxeflora.listener.FirestoreCallback;
import lk.sasindi.luxeflora.model.CartItem;
import lk.sasindi.luxeflora.model.Order;
import lk.sasindi.luxeflora.model.Product;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;

    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private double total;
    private boolean paymentActive;

    private boolean isBuyNow = false;
    private String buyNowProductId;
    private int buyNowQuantity = 1;

    private List<String> cityList = new ArrayList<>();
    private ArrayAdapter<String> cityAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            isBuyNow = getArguments().getBoolean("isBuyNow", false);
            buyNowProductId = getArguments().getString("productId");
            buyNowQuantity = getArguments().getInt("quantity", 1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);

        binding.shippingLayoutBtn.setOnClickListener(v -> {
            if (binding.shippingLayoutBody.getVisibility() == View.GONE) {
                binding.shippingLayoutBody.setVisibility(View.VISIBLE);
                binding.shippingLayoutBtn.setRotation(180f);
            } else {
                binding.shippingLayoutBody.setVisibility(View.GONE);
                binding.shippingLayoutBtn.setRotation(0f);
            }
        });

        binding.billingLayoutBtn.setOnClickListener(v -> {
            if (binding.billingLayoutBody.getVisibility() == View.GONE) {
                binding.shippingLayoutBody.setVisibility(View.VISIBLE);
                binding.billingLayoutBtn.setRotation(180f);
            } else {
                binding.billingLayoutBody.setVisibility(View.GONE);
                binding.billingLayoutBtn.setRotation(0f);
            }
        });

        binding.shippingDetailsCheckBilling.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.billingLayout.setVisibility(View.GONE);
            } else {
                binding.billingLayout.setVisibility(View.VISIBLE);
                binding.billingLayoutBody.setVisibility(View.VISIBLE);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        double shippingCost = 400;

        setupCityDropdown();
        loadCitiesFromFirestore();

        if (isBuyNow) {

            List<String> productIds = new ArrayList<>();
            productIds.add(buyNowProductId);

            getProductsById(productIds, data -> {

                Product product = data.get(buyNowProductId);

                if (product != null) {

                    double subTotal = product.getPrice() * buyNowQuantity;

                    total = subTotal + shippingCost;

                    binding.checkoutSubtotal.setText(String.format(Locale.US, "LKR %,.2f", subTotal));
                    binding.checkoutShipping.setText(String.format(Locale.US, "LKR %,.2f", shippingCost));
                    binding.checkoutTotal.setText(String.format(Locale.US, "LKR %,.2f", total));

                    paymentActive = true;
                }
            });

        } else {

            getCartItems(cartItems -> {
                ArrayList<String> productIds = new ArrayList<>();
                cartItems.forEach(cartItem -> {
                    productIds.add(cartItem.getProductId());
                });
                getProductsById(productIds, data -> {
                    double subTotal = 0;

                    for (CartItem cartItem : cartItems) {
                        Product product = data.get(cartItem.getProductId());
                        if (product != null) {
                            subTotal += product.getPrice() * cartItem.getQuantity();
                        }
                    }

                    total = subTotal + shippingCost;
                    binding.checkoutSubtotal.setText(String.format(Locale.US, "LKR %,.2f", subTotal));
                    binding.checkoutShipping.setText(String.format(Locale.US, "LKR %,.2f", shippingCost));
                    binding.checkoutTotal.setText(String.format(Locale.US, "LKR %,.2f", total));
                    paymentActive = true;
                });
            });
        }

        binding.shippingUseProfile.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                loadProfileToShipping();

                setShippingFieldsEnabled(false);

            } else {

                binding.shippingDetailsName.setText("");
                binding.shippingDetailsEmail.setText("");
                binding.shippingDetailsContact.setText("");
                binding.shippingDetailsAddress1.setText("");
                binding.shippingDetailsAddress2.setText("");
                binding.shippingDetailsCity.setText("");
                binding.shippingDetailsPostalCode.setText("");

                setShippingFieldsEnabled(true);
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {

                        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                            getParentFragmentManager().popBackStack(); // go back
                        } else {
                            getParentFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, new HomeFragment())
                                    .commit();
                        }
                    }
                });

        binding.checkoutBtnProceed.setOnClickListener(v -> {

            if (paymentActive && validateFields()) {
                InitRequest req = new InitRequest();
                req.setSandBox(true);

                req.setMerchantId("1224066");
                req.setMerchantSecret("MERCHANT_SECRET");
                req.setCurrency("LKR");
                req.setAmount(total);
                req.setOrderId("ESOI-001");
                req.setItemsDescription("flowers");

                req.getCustomer().setFirstName(binding.shippingDetailsName.getText().toString());
                req.getCustomer().setLastName("Perera");
                req.getCustomer().setEmail("samanp@gmail.com");
                req.getCustomer().setPhone("+94771234567");
                req.getCustomer().getAddress().setAddress("No.1, Galle Road");
                req.getCustomer().getAddress().setCity("Colombo");
                req.getCustomer().getAddress().setCountry("Sri Lanka");

                Intent intent = new Intent(getActivity(), PHMainActivity.class);
                intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

                payhereLauncher.launch(intent);
            }
        });
    }


    private void saveOrder(StatusResponse statusResponse) {

        String uid = firebaseAuth.getCurrentUser().getUid();

        Order order = new Order();
        order.setOrderId(String.valueOf(System.currentTimeMillis()));
        order.setUserId(uid);
        order.setTotalAmount(total);
        order.setStatus("Pending");
        order.setOrderDate(Timestamp.now());

        Order.Address shippingAddress = Order.Address.builder()
                .name(binding.shippingDetailsName.getText().toString())
                .email(binding.shippingDetailsEmail.getText().toString())
                .contact(binding.shippingDetailsContact.getText().toString())
                .address1(binding.shippingDetailsAddress1.getText().toString())
                .address2(binding.shippingDetailsAddress2.getText().toString())
                .city(binding.shippingDetailsCity.getText().toString())
                .postcode(binding.shippingDetailsPostalCode.getText().toString())
                .build();

        order.setShippingAddress(shippingAddress);

        if (isBuyNow) {

            List<String> productIds = new ArrayList<>();
            productIds.add(buyNowProductId);

            getProductsById(productIds, productMap -> {

                List<Order.OrderItem> orderItems = new ArrayList<>();

                Product product = productMap.get(buyNowProductId);

                if (product != null) {
                    orderItems.add(Order.OrderItem.builder()
                            .productId(buyNowProductId)
                            .productName(product.getTitle())
                            .productImage(product.getImages() != null && !product.getImages().isEmpty()
                                    ? product.getImages().get(0)
                                    : "")
                            .unitPrice(product.getPrice())
                            .quantity(buyNowQuantity)
                            .attributes(new ArrayList<>())
                            .build());
                }

                order.setOrderItems(orderItems);

                db.collection("orders")
                        .document(order.getOrderId())
                        .set(order)
                        .addOnSuccessListener(unused -> {

                            Toast.makeText(getContext(), "Order Saved", Toast.LENGTH_SHORT).show();

                            getParentFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, new HomeFragment())
                                    .commit();
                        });
            });

        } else {

            getCartItems(cartItems -> {

                List<String> productIds = new ArrayList<>();
                for (CartItem cartItem : cartItems) {
                    productIds.add(cartItem.getProductId());
                }

                getProductsById(productIds, productMap -> {

                    List<Order.OrderItem> orderItems = new ArrayList<>();

                    for (CartItem cartItem : cartItems) {

                        Product product = productMap.get(cartItem.getProductId());

                        if (product != null) {
                            String imageUrl = "";

                            if (product.getImages() != null && !product.getImages().isEmpty()) {
                                imageUrl = product.getImages().get(0);
                            }

                            orderItems.add(Order.OrderItem.builder()
                                    .productId(cartItem.getProductId())
                                    .productName(product.getTitle())
                                    .productImage(imageUrl)
                                    .unitPrice(product.getPrice())
                                    .quantity(cartItem.getQuantity())
                                    .attributes(new ArrayList<>())
                                    .build());
                        }
                    }

                    order.setOrderItems(orderItems);

                    db.collection("orders")
                            .document(order.getOrderId())
                            .set(order)
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(getContext(), "Order Saved", Toast.LENGTH_SHORT).show();

                                // OPTIONAL: clear cart after order
                                clearCart();

                                getParentFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.fragment_container, new HomeFragment())
                                        .commit();
                            });
                });
            });
        }
    }

    private void clearCart() {

        String uid = firebaseAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("cart")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    for (var doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                });
    }

    private void setShippingFieldsEnabled(boolean enabled) {

        binding.shippingDetailsName.setEnabled(enabled);
        binding.shippingDetailsEmail.setEnabled(enabled);
        binding.shippingDetailsContact.setEnabled(enabled);
        binding.shippingDetailsAddress1.setEnabled(enabled);
        binding.shippingDetailsAddress2.setEnabled(enabled);
        binding.shippingDetailsCity.setEnabled(enabled);
        binding.shippingDetailsPostalCode.setEnabled(enabled);
    }

    private void setupCityDropdown() {

        cityAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                cityList
        );

        binding.shippingDetailsCity.setAdapter(cityAdapter);
        binding.billingDetailsCity.setAdapter(cityAdapter);

        binding.shippingDetailsCity.setOnClickListener(v ->
                binding.shippingDetailsCity.showDropDown()
        );

        binding.billingDetailsCity.setOnClickListener(v ->
                binding.billingDetailsCity.showDropDown()
        );

        binding.shippingDetailsCity.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.shippingDetailsCity.showDropDown();
        });

        binding.billingDetailsCity.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.billingDetailsCity.showDropDown();
        });
    }

    private void loadCitiesFromFirestore() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("cities")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    cityList.clear();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        List<String> cities = (List<String>) doc.get("name");

                        if (cities != null) {
                            cityList.addAll(cities);
                        }
                    }

                    cityAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Failed to load cities",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfileToShipping() {

        String uid = firebaseAuth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String contact = documentSnapshot.getString("contactNo");
                        String address1 = documentSnapshot.getString("addressLine1");
                        String address2 = documentSnapshot.getString("addressLine2");
                        String city = documentSnapshot.getString("city");
                        String postal = documentSnapshot.getString("postalCode");

                        binding.shippingDetailsName.setText(name);
                        binding.shippingDetailsEmail.setText(email);
                        binding.shippingDetailsContact.setText(contact);
                        binding.shippingDetailsAddress1.setText(address1);
                        binding.shippingDetailsAddress2.setText(address2);
                        binding.shippingDetailsCity.setText(city, false);
                        binding.shippingDetailsPostalCode.setText(postal);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void getCartItems(FirestoreCallback<List<CartItem>> callback) {

        String uid = firebaseAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).collection("cart")
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {
                        if (!qds.isEmpty()) {

                            List<CartItem> cartItems = qds.toObjects(CartItem.class);
                            callback.onCallback(cartItems);


                        }
                    }
                });

    }

    private boolean validateFields() {


        String name = binding.shippingDetailsName.getText().toString().trim();
        String email = binding.shippingDetailsEmail.getText().toString().trim();
        String phone = binding.shippingDetailsContact.getText().toString().trim();
        String address1 = binding.shippingDetailsAddress1.getText().toString().trim();
        String address2 = binding.shippingDetailsAddress2.getText().toString().trim();
        String city = binding.shippingDetailsCity.getText().toString().trim();
        String postal = binding.shippingDetailsPostalCode.getText().toString().trim();

        if (name.isEmpty()) {
            binding.shippingDetailsName.setError("Name Required");
            binding.shippingDetailsName.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            binding.shippingDetailsEmail.setError("Email Required");
            binding.shippingDetailsEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.shippingDetailsEmail.setError("Invalid Email");
            binding.shippingDetailsEmail.requestFocus();
            return false;
        }

        if (phone.isEmpty()) {
            binding.shippingDetailsContact.setError("Mobile Number Required");
            binding.shippingDetailsContact.requestFocus();
            return false;
        }

        if (!phone.matches("^(07\\d{8})$")) {
            binding.shippingDetailsContact.setError("Invalid Phone Number");
            binding.shippingDetailsContact.requestFocus();
            return false;
        }

        if (address1.isEmpty()) {
            binding.shippingDetailsAddress1.setError("Address Line 1 Required");
            binding.shippingDetailsAddress1.requestFocus();
            return false;
        }
        if (address2.isEmpty()) {
            binding.shippingDetailsAddress2.setError("Address Line 2 Required");
            binding.shippingDetailsAddress2.requestFocus();
            return false;
        }

        if (city.isEmpty()) {
            binding.shippingDetailsCity.setError("Select City");
            binding.shippingDetailsCity.requestFocus();
            return false;
        }

        if (!cityList.contains(city)) {
            binding.shippingDetailsCity.setError("Invalid City");
            binding.shippingDetailsCity.requestFocus();
            return false;
        }

        if (postal.isEmpty()) {
            binding.shippingDetailsPostalCode.setError("Postal code Required");
            binding.shippingDetailsPostalCode.requestFocus();
            return false;
        }


        if (!binding.shippingDetailsCheckBilling.isChecked()) {

            String bName = binding.billingDetailsName.getText().toString().trim();
            String bEmail = binding.billingDetailsEmail.getText().toString().trim();
            String bPhone = binding.billingDetailsContact.getText().toString().trim();
            String bAddress1 = binding.billingDetailsAddress1.getText().toString().trim();
            String bAddress2 = binding.billingDetailsAddress2.getText().toString().trim();
            String bCity = binding.billingDetailsCity.getText().toString().trim();
            String bPostal = binding.billingDetailsPostalCode.getText().toString().trim();

            if (bName.isEmpty()) {
                binding.billingDetailsName.setError("Name Required");
                binding.billingDetailsName.requestFocus();
                return false;
            }

            if (bEmail.isEmpty()) {
                binding.billingDetailsEmail.setError("Email Required");
                binding.billingDetailsEmail.requestFocus();
                return false;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(bEmail).matches()) {
                binding.billingDetailsEmail.setError("Invalid Email");
                binding.billingDetailsEmail.requestFocus();
                return false;
            }

            if (bPhone.isEmpty()) {
                binding.billingDetailsContact.setError("Mobile Number Required");
                binding.billingDetailsContact.requestFocus();
                return false;
            }

            if (!bPhone.matches("^(07\\d{8})$")) {
                binding.billingDetailsContact.setError("Invalid Mobile Number");
                binding.billingDetailsContact.requestFocus();
                return false;
            }

            if (bAddress1.isEmpty()) {
                binding.billingDetailsAddress1.setError("Address line 1 Required");
                binding.billingDetailsAddress1.requestFocus();
                return false;
            }

            if (bAddress2.isEmpty()) {
                binding.billingDetailsAddress2.setError("Address line 2 Required");
                binding.billingDetailsAddress2.requestFocus();
                return false;
            }

            if (bCity.isEmpty()) {
                binding.billingDetailsCity.setError("Select City");
                binding.billingDetailsCity.requestFocus();
                return false;
            }

            if (!cityList.contains(bCity)) {
                binding.billingDetailsCity.setError("Invalid City");
                binding.billingDetailsCity.requestFocus();
                return false;
            }

            if (bPostal.isEmpty()) {
                binding.billingDetailsPostalCode.setError("Postal Code Required");
                binding.billingDetailsPostalCode.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void getProductsById(List<String> productIds, FirestoreCallback<Map<String, Product>> callback) {

        Map<String, Product> products = new HashMap<>();

        if (productIds == null || productIds.isEmpty()) {
            callback.onCallback(products);
            return;
        }

        db.collection("products")
                .whereIn("productId", productIds)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {

                        qds.getDocuments().forEach(ds -> {
                            Product product = ds.toObject(Product.class);
                            if (product != null) {
                                products.put(product.getProductId(), product);
                            }
                        });
                        callback.onCallback(products);
                    }
                });

    }


    private final ActivityResultLauncher<Intent> payhereLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                        PHResponse<StatusResponse> response =
                                (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                        if (response != null && response.isSuccess()) {
                            saveOrder(response.getData());
                        }
                    }
                }
            });
}