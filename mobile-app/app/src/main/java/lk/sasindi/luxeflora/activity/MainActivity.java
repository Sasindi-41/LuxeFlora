package lk.sasindi.luxeflora.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.databinding.ActivityMainBinding;
import lk.sasindi.luxeflora.databinding.SideNavHeaderBinding;
import lk.sasindi.luxeflora.fragment.CartFragment;
import lk.sasindi.luxeflora.fragment.CategoryFragment;
import lk.sasindi.luxeflora.fragment.HomeFragment;
import lk.sasindi.luxeflora.fragment.ListingFragment;
import lk.sasindi.luxeflora.fragment.LocationFragment;
import lk.sasindi.luxeflora.fragment.MessageFragment;
import lk.sasindi.luxeflora.fragment.OrdersFragment;
import lk.sasindi.luxeflora.fragment.ProfileFragment;
import lk.sasindi.luxeflora.fragment.SettingsFragment;
import lk.sasindi.luxeflora.fragment.WishlistFragment;
import lk.sasindi.luxeflora.model.User;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private SideNavHeaderBinding sideNavHeaderBinding;

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private EditText searchInput;
    private ImageButton filterBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View headerView = binding.sideNavigationView.getHeaderView(0);
        sideNavHeaderBinding = SideNavHeaderBinding.bind(headerView);

        drawerLayout = binding.drawerLayout;
        toolbar = binding.toolbar;
        navigationView = binding.sideNavigationView;
        bottomNavigationView = binding.bottomNavigationView;

        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        android.Manifest.permission.POST_NOTIFICATIONS
                }, 101);
            }
        }

        searchInput = toolbar.findViewById(R.id.text_input_search);
        filterBtn = toolbar.findViewById(R.id.btn_filter);

        filterBtn.setOnClickListener(v -> {

            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);

            android.widget.PopupMenu popup = new android.widget.PopupMenu(MainActivity.this, filterBtn);

            if (currentFragment instanceof HomeFragment) {
                popup.getMenu().add("Name ASC");
                popup.getMenu().add("Name DSC");
                popup.getMenu().add("Price Low to High");
                popup.getMenu().add("Price High to Low");

            } else if (currentFragment instanceof CategoryFragment) {
                popup.getMenu().add("Name ASC");
                popup.getMenu().add("Name DSC");
            }

            popup.setOnMenuItemClickListener(item -> {

                if (currentFragment instanceof HomeFragment) {
                    ((HomeFragment) currentFragment).sortProducts(item.getTitle().toString());

                } else if (currentFragment instanceof CategoryFragment) {
                    if (item.getTitle().equals("Name ASC")) {
                        ((CategoryFragment) currentFragment).sortCategories(true);
                    } else {
                        ((CategoryFragment) currentFragment).sortCategories(false);
                    }
                }

                return true;
            });

            popup.show();
        });

        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                        R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                    drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
                } else if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    finish();
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
            navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // LOAD USER DATA
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            firebaseFirestore.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            sideNavHeaderBinding.headerUserName.setText(user.getName());
                            sideNavHeaderBinding.headerUserEmail.setText(user.getEmail());

                            if (user.getProfilePicUrl() != null &&
                                    !user.getProfilePicUrl().isEmpty()) {
                                Glide.with(MainActivity.this)
                                        .load(user.getProfilePicUrl())
                                        .circleCrop()
                                        .into(sideNavHeaderBinding.headerProfilePic);
                            }
                        }
                    });

            navigationView.getMenu().findItem(R.id.side_nav_login).setVisible(false);
            navigationView.getMenu().findItem(R.id.side_nav_profile).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_orders).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_wishlist).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_cart).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_message).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_logout).setVisible(true);
        }

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                Fragment currentFragment = getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);

                String query = s.toString().trim();

                if (currentFragment instanceof HomeFragment) {

                    ((HomeFragment) currentFragment).filterProducts(query);

                } else if (currentFragment instanceof CategoryFragment) {

                    ((CategoryFragment) currentFragment).filterCategories(query);

                } else if (currentFragment instanceof ListingFragment) {

                    ((ListingFragment) currentFragment).filterProducts(query);
                }else if (currentFragment instanceof CartFragment) {
                    ((CartFragment) currentFragment).filterCart(s.toString().trim());

                } else if (currentFragment instanceof WishlistFragment) {
                    ((WishlistFragment) currentFragment).filterWishlist(s.toString().trim());
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();

        if (itemId == R.id.side_nav_home || itemId == R.id.bottom_nav_home) {
            loadFragment(new HomeFragment(), false);

        } else if (itemId == R.id.side_nav_profile || itemId == R.id.bottom_nav_profile) {
            loadFragment(new ProfileFragment(), true);

        } else if (itemId == R.id.side_nav_orders) {
            loadFragment(new OrdersFragment(), true);

        } else if (itemId == R.id.side_nav_wishlist) {
            loadFragment(new WishlistFragment(), true);

        } else if (itemId == R.id.side_nav_cart || itemId == R.id.bottom_nav_cart) {
            loadFragment(new CartFragment(), true);

        } else if (itemId == R.id.side_nav_message) {
            loadFragment(new MessageFragment(), true);

        } else if (itemId == R.id.side_nav_settings) {
            loadFragment(new SettingsFragment(), true);

        } else if (itemId == R.id.side_nav_location) {
            loadFragment(new LocationFragment(), true);

        } else if (itemId == R.id.bottom_nav_category) {
            loadFragment(new CategoryFragment(), true);

        } else if (itemId == R.id.side_nav_login) {
            startActivity(new Intent(MainActivity.this, SignInActivity.class));

        } else if (itemId == R.id.side_nav_logout) {
            firebaseAuth.signOut();
            loadFragment(new HomeFragment(), false);

            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.side_nav_menu);

            navigationView.removeHeaderView(sideNavHeaderBinding.getRoot());
            navigationView.inflateHeaderView(R.layout.side_nav_header);
        }

        if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
        }

        return true;
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) transaction.addToBackStack(null);
        transaction.commit();
    }
}