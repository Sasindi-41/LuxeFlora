package lk.sasindi.luxeflora.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.model.User;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

public class ProfileFragment extends Fragment {

    private ImageView profilePic;
    private ShapeableImageView profile;
    private EditText name, email, contact, address1, address2, postal;
    private AutoCompleteTextView city;
    private MaterialButton saveBtn;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private List<String> cityList = new ArrayList<>();
    private ArrayAdapter<String> cityAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profilePic = view.findViewById(R.id.profile_pic);
        profile = view.findViewById(R.id.profile);
        name = view.findViewById(R.id.profile_input_name);
        email = view.findViewById(R.id.profile_input_email);
        contact = view.findViewById(R.id.profile_input_contact_no);
        address1 = view.findViewById(R.id.profile_input_address_line1);
        address2 = view.findViewById(R.id.profile_input_address_line2);
        city = view.findViewById(R.id.profile_input_city);
        postal = view.findViewById(R.id.profile_input_postal);
        saveBtn = view.findViewById(R.id.profile_save_changes);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        email.setEnabled(false);

        setupBackButton();
        setupImagePicker();
        loadUserData();

        profilePic.setOnClickListener(v -> openImagePicker());
        saveBtn.setOnClickListener(v -> saveProfileChanges());

        setupCityDropdown();
        loadCitiesFromFirestore();

        return view;
    }

    private void setupCityDropdown() {

        cityAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                cityList
        );

        city.setAdapter(cityAdapter);

        city.setOnClickListener(v -> city.showDropDown());

        city.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) city.showDropDown();
        });

        city.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = parent.getItemAtPosition(position).toString();
            Toast.makeText(requireContext(), selectedCity, Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCitiesFromFirestore() {

        firestore.collection("cities")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    cityList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {

                        List<String> cities = (List<String>) doc.get("name");

                        if (cities != null) cityList.addAll(cities);
                    }

                    cityAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load cities", Toast.LENGTH_SHORT).show());
    }

    private void setupBackButton() {

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {

                        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                            getParentFragmentManager().popBackStack();
                        } else {
                            getParentFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, new HomeFragment())
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }
                });
    }

    private void loadUserData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        firestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        User user = documentSnapshot.toObject(User.class);

                        if (user != null) {

                            name.setText(user.getName());
                            email.setText(user.getEmail());
                            contact.setText(user.getContactNo());
                            address1.setText(user.getAddressLine1());
                            address2.setText(user.getAddressLine2());
                            city.setText(user.getCity(), false);
                            postal.setText(user.getPostalCode());

                            if (!TextUtils.isEmpty(user.getProfilePicUrl())) {

                                Glide.with(requireContext())
                                        .load(user.getProfilePicUrl())
                                        .circleCrop()
                                        .into(profile);
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("ProfileFragment", "Error loading user: " + e.getMessage()));
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();

                        Glide.with(requireContext())
                                .load(selectedImageUri)
                                .circleCrop()
                                .into(profile);
                    }
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(intent);
    }

    private void saveProfileChanges() {

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        String userName = name.getText().toString().trim();
        String contactNo = contact.getText().toString().trim();
        String addr1 = address1.getText().toString().trim();
        String addr2 = address2.getText().toString().trim();
        String cityTxt = city.getText().toString().trim();
        String postalTxt = postal.getText().toString().trim();

        if (TextUtils.isEmpty(userName)) {
            name.setError("Name required");
            return;
        } else if (TextUtils.isEmpty(contactNo)) {
            contact.setError("Mobile Number required");
            return;
        } else if (!contactNo.matches("^07[0-9]{8}$")) {
            contact.setError("Enter valid mobile number (e.g., 0771234567)");
            return;
        }

        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) return;

                    String oldName = snapshot.getString("name");
                    String oldContact = snapshot.getString("contactNo");
                    String oldAddr1 = snapshot.getString("addressLine1");
                    String oldAddr2 = snapshot.getString("addressLine2");
                    String oldCity = snapshot.getString("city");
                    String oldPostal = snapshot.getString("postalCode");

                    if (userName.equals(oldName) &&
                            contactNo.equals(oldContact) &&
                            addr1.equals(oldAddr1) &&
                            addr2.equals(oldAddr2) &&
                            cityTxt.equals(oldCity) &&
                            postalTxt.equals(oldPostal) &&
                            selectedImageUri == null) {

                        Toast.makeText(requireContext(),
                                "This data already exists",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }


                    if (selectedImageUri != null) {

                        new Thread(() -> {
                            try {

                                String cloudName = "CLOUD_NAME";
                                String uploadPreset = "product_upload";

                                String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";

                                InputStream inputStream =
                                        requireContext().getContentResolver().openInputStream(selectedImageUri);

                                byte[] bytes = new byte[inputStream.available()];
                                inputStream.read(bytes);
                                inputStream.close();

                                OkHttpClient client = new OkHttpClient();

                                RequestBody requestBody = new MultipartBody.Builder()
                                        .setType(MultipartBody.FORM)
                                        .addFormDataPart("file", "profile.jpg",
                                                RequestBody.create(bytes))
                                        .addFormDataPart("upload_preset", uploadPreset)
                                        .build();

                                Request request = new Request.Builder()
                                        .url(uploadUrl)
                                        .post(requestBody)
                                        .build();

                                Response response = client.newCall(request).execute();
                                String result = response.body().string();

                                JSONObject json = new JSONObject(result);
                                String imageUrl = json.getString("secure_url");

                                requireActivity().runOnUiThread(() -> {

                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("name", userName);
                                    updates.put("contactNo", contactNo);
                                    updates.put("addressLine1", addr1);
                                    updates.put("addressLine2", addr2);
                                    updates.put("city", cityTxt);
                                    updates.put("postalCode", postalTxt);
                                    updates.put("profilePicUrl", imageUrl);

                                    firestore.collection("users")
                                            .document(uid)
                                            .update(updates)
                                            .addOnSuccessListener(unused ->
                                                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();

                    } else {

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("name", userName);
                        updates.put("contactNo", contactNo);
                        updates.put("addressLine1", addr1);
                        updates.put("addressLine2", addr2);
                        updates.put("city", cityTxt);
                        updates.put("postalCode", postalTxt);

                        firestore.collection("users").document(uid)
                                .update(updates)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                });
    }
}