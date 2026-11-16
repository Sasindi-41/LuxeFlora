package lk.sasindi.luxeflora.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import lk.sasindi.luxeflora.MapsActivity;
import lk.sasindi.luxeflora.R;

public class LocationFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap miniMap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_location, container, false);

        Button btnDirections = view.findViewById(R.id.btnDirections);

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.map_preview_container, mapFragment)
                .commit();

        mapFragment.getMapAsync(this);

        btnDirections.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MapsActivity.class);
            intent.putExtra("show_direction", true);
            startActivity(intent);
        });

        FloatingActionButton fabCall = view.findViewById(R.id.fabCall);
        fabCall.setOnClickListener(v -> {
            try {
                TextView contactTextView = view.findViewById(R.id.contact_text);
                String phoneNumber = contactTextView.getText().toString().trim();

                if (!phoneNumber.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), "No contact number available", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Unable to open dialer", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                            getParentFragmentManager().popBackStack();
                        } else {
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                }
        );

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        miniMap = googleMap;

        LatLng shop = new LatLng(6.9789259, 79.9093471);

        miniMap.addMarker(new MarkerOptions()
                .position(shop)
                .title("Luxe Flora Shop"));

        miniMap.moveCamera(CameraUpdateFactory.newLatLngZoom(shop, 15f));
        miniMap.getUiSettings().setAllGesturesEnabled(false);
    }
}