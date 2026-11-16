package lk.sasindi.luxeflora;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private LatLng currentLocation;
    private Marker shopMarker;
    private Polyline polyline;

    private final ExecutorService service = Executors.newSingleThreadExecutor();

    private boolean directionRequested = false;
    private boolean locationReady = false;

    private boolean firstLocationShown = false;

    private final LatLng shopLocation =
            new LatLng(6.9789259, 79.9093471);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        directionRequested = getIntent().getBooleanExtra("show_direction", false);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        enableMyLocation();

        shopMarker = mMap.addMarker(new MarkerOptions()
                .position(shopLocation)
                .title("Luxe Flora Shop")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(shopLocation, 15f));

        mMap.setOnMyLocationButtonClickListener(() -> {

            if (!isLocationEnabled() || currentLocation == null) {
                showLocationDialog();
                return true;
            }

            directionRequested = true;

            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLocation, 15f)
            );

            getDirection(currentLocation, shopLocation);

            return true;
        });
    }


    private void enableMyLocation() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
            startLocationUpdate();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void startLocationUpdate() {

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult result) {

                for (Location location : result.getLocations()) {

                    currentLocation = new LatLng(
                            location.getLatitude(),
                            location.getLongitude()
                    );

                    locationReady = true;

                    if (!firstLocationShown) {

                        firstLocationShown = true;

                        mMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(currentLocation, 15f)
                        );
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                getMainLooper()
        );
    }


    public void getDirection(LatLng start, LatLng end) {

        service.execute(() -> {

            String key = "MAPS_API_KEY";

            String origin = start.latitude + "," + start.longitude;
            String destination = end.latitude + "," + end.longitude;

            try (GeoApiContext context =
                         new GeoApiContext.Builder().apiKey(key).build()) {

                DirectionsResult result = DirectionsApi.newRequest(context)
                        .mode(TravelMode.DRIVING)
                        .origin(origin)
                        .destination(destination)
                        .await();

                if (result.routes.length > 0) {

                    String encodedPath =
                            result.routes[0].overviewPolyline.getEncodedPath();

                    List<LatLng> points = PolyUtil.decode(encodedPath);

                    runOnUiThread(() -> {

                        if (polyline == null) {
                            polyline = mMap.addPolyline(
                                    new PolylineOptions()
                                            .width(12)
                                            .color(ContextCompat.getColor(
                                                    this,
                                                    R.color.md_theme_primary))
                                            .addAll(points)
                            );
                        } else {
                            polyline.setPoints(points);
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private boolean isLocationEnabled() {

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showLocationDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Location Required")
                .setMessage("Please enable location to get directions")
                .setPositiveButton("Open Settings",
                        (d, w) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Cancel", null)
                .show();
    }
}