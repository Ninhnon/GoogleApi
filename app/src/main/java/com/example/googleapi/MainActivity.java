package com.example.googleapi;

import static androidx.core.content.ContentProviderCompat.requireContext;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.googleapi.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, RoutingListener {
    ActivityMainBinding binding;
    GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentPosition;
    public static LatLng end = null;
    public List<LatLng> listEnd = new ArrayList<>();
    MarkerOptions markerEnd;
    //to get location permissions.
    private final static int LOCATION_REQUEST_CODE = 44;
    //polyline object
    private List<Polyline> polylines = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        binding.optionMenu.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(this, view);
            popup.setOnMenuItemClickListener(this);
            popup.inflate(R.menu.menu_map_type);
            popup.show();
        });
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(callback);

        // btn find route
        binding.fabRoute.setOnClickListener(v -> {
            // tim duong di giua hai diem
            //start route finding

            if (listEnd != null) {
                listEnd.add(currentPosition);
                for (int i = 1; i<listEnd.size();i++) {
                    Findroutes(listEnd.get(i-1),listEnd.get(i));
                }
            }
        });

        binding.btnEraser.setOnClickListener(v -> {
            map.clear();
            listEnd.clear();
        });

        // search
        onHandleSearch();
        setContentView(binding.getRoot());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mapNone:
                map.setMapType(GoogleMap.MAP_TYPE_NONE);
                return true;
            case R.id.mapNormal:
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;
            case R.id.mapSatellite:
                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                return true;
            case R.id.mapHybrid:
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                return true;
            case R.id.mapTerrain:
                map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                return true;
            default:
                return false;
        }
    }

    boolean isPermissionGranted() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }

    private final OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;
            map.setPadding(0, 120, 0, 0);
            // lay vi tri hien tai
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
            if (isPermissionGranted()) {
                getCurrentLocation();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }

            // click mot diem => lay thong tin tai diem do
            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull LatLng latLng) {

                    Geocoder geocoder = new Geocoder(MainActivity.this);
                    try {
                        end = latLng;
                        listEnd.add(end);
                        // lay thong tin tai diem do
                        ArrayList<Address> addresses = (ArrayList<Address>) geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                        ShowSnackbar(addresses.get(0).getAddressLine(0)); // show info address
                        markerEnd = new MarkerOptions().position(latLng).title(addresses.get(0).getAddressLine(0));
                        map.addMarker(markerEnd);

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Không xác định được vị trí", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        // ham lay toa do hien tai
        if (isPermissionGranted()) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        // init lat lng
                        currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
                        // create marker
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15));
                        map.getUiSettings().setZoomControlsEnabled(true);
                        map.getUiSettings().setMapToolbarEnabled(true);
                        map.getUiSettings().setMyLocationButtonEnabled(true);
                        map.setMyLocationEnabled(true);
                        map.setBuildingsEnabled(true);
                    }
                }
            });
        }
    }

    // function to find Routes.
    public void Findroutes(LatLng Start, LatLng End) {
        if (Start == null || End == null) {
            Toast.makeText(MainActivity.this, "Unable to get location", Toast.LENGTH_LONG).show();
        } else {

            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(Start, End)
                    .key(getString(R.string.api_key))  //also define your api key here.
                    .build();
            routing.execute();
        }
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        ShowToast("Error" + e.getMessage());
        Log.d("Error", e.getMessage());
    }

    @Override
    public void onRoutingStart() {
        ShowToast("Finding Route...");
    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        // thanh cong => ve duong di
        CameraUpdate center = CameraUpdateFactory.newLatLng(currentPosition);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
        if (polylines != null) {
            polylines.clear();
        }
        PolylineOptions polyOptions = new PolylineOptions();
        LatLng polylineStartLatLng = null;
        LatLng polylineEndLatLng = null;


        polylines = new ArrayList<>();
        //add route(s) to the map using polyline
        for (int i = 0; i < route.size(); i++) {

            if (i == shortestRouteIndex) {
                polyOptions.color(Color.BLUE);
                polyOptions.width(7);
                polyOptions.addAll(route.get(shortestRouteIndex).getPoints());
                Polyline polyline = map.addPolyline(polyOptions);
                polylineStartLatLng = polyline.getPoints().get(0);
                int k = polyline.getPoints().size();
                polylineEndLatLng = polyline.getPoints().get(k - 1);
                polylines.add(polyline);
            } else {

            }

        }
    }

    @Override
    public void onRoutingCancelled() {
        if (listEnd != null) {
            listEnd.add(currentPosition);
            for (int i = 1; i<listEnd.size();i++) {
                Findroutes(listEnd.get(i-1),listEnd.get(i));
            }
        }
    }
    

    private void onHandleSearch() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {

                String location = binding.searchView.getQuery().toString();
                List<Address> addresses = null;

                if (location != null) {
                    Geocoder geocoder = new Geocoder(MainActivity.this);

                    try {
                        addresses = geocoder.getFromLocationName(location, 1);

                        if (addresses != null) {
                            Address address = addresses.get(0);
                            end = new LatLng(address.getLatitude(), address.getLongitude());
                            // add this marker to map
                            map.clear();
                            markerEnd = new MarkerOptions().position(end).title(location);

                            map.addMarker(markerEnd);
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(end, 19));
                        }
                    } catch (Exception e) {
                        ShowToast("Không tìm ra địa điểm: " + e.getMessage());
                    }
                } else {
                    ShowToast("Vui lòng nhập gì đó");
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    public void ShowToast(String value) {
        Toast.makeText(MainActivity.this, value, Toast.LENGTH_SHORT).show();
    }

    public void ShowSnackbar(String value) {
        Snackbar snackbar = Snackbar.make(binding.baseLayout, value, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(MainActivity.this, R.color.white))
                .setTextColor(ContextCompat.getColor(MainActivity.this, R.color.black));
        snackbar.show();
    }
}