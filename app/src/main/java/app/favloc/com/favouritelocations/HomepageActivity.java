package app.favloc.com.favouritelocations;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.Result;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class HomepageActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, NavigationView.OnNavigationItemSelectedListener, ZXingScannerView.ResultHandler {

    private Button saveButton;
    private EditText nameOfLocation, landmarkOfLocation;
    private TextView locCount;
    private ImageView profileImage;
    private Button shareCurrentLocation;

    private FirebaseAuth firebaseAuth;
    private LoginManager loginManager;
    private DatabaseReference databaseReference, dbCountReference;
    private StorageReference storageReference;

    private GoogleMap mGoogleMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationManager locationManager;

    private AlertDialog.Builder alertDialog, alertDialog2, locationAlreadyPresentDialog;
    private ProgressDialog uploadImageDialog, removeImageDialog;
    private Uri profileUri;

    private String currentLat, currentLng;
    private static int LocationCount = 0;
    private static final int GALLERY_INTENT = 2, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 5, LOCATION_PERMISSION_GRANTED = 2000, CAMERA_PERMISSION_GRANTED = 1255;
    private int uploadCount = 0;
    private boolean facebookUser = false, cameraOpen = false;
    private String facebookUserId, photoUri;

    private Marker marker;
    private ZXingScannerView scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (googlePlayServicesAvailable()) {
            setContentView(R.layout.nav_drawer_layout);
            //Toast.makeText(getApplicationContext(), "Perfect", Toast.LENGTH_LONG).show();
            initMap();
        } else
            Toast.makeText(getApplicationContext(), "Google map is not supported.", Toast.LENGTH_LONG).show();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        init();

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayoutID);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navView);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        profileImage = (ImageView) headerView.findViewById(R.id.profileImageId);
        TextView title = (TextView) headerView.findViewById(R.id.profileNameID);
        title.setText(firebaseAuth.getCurrentUser().getDisplayName());
        for (UserInfo info : firebaseAuth.getCurrentUser().getProviderData()) {
            if (info.getProviderId().equals("facebook.com")) {
                facebookUser = true;
                facebookUserId = info.getUid();
                photoUri = "https://graph.facebook.com/" + facebookUserId + "/picture?height=500";
            } else
                facebookUser = false;
        }

        if (facebookUser)
            Picasso.with(this).load(photoUri).into(profileImage);
        else {
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Photos").child(firebaseAuth.getCurrentUser().getEmail());
            try {
                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Picasso.with(getApplicationContext()).load(uri.toString()).centerCrop().fit().into(profileImage);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        profileImage.setImageResource(R.drawable.profileicon);
                    }
                });
            } catch (Exception e) {
            }
        }

        locCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locCount.getText().toString().equals("You have not saved any favourite location yet")) {
                    alertDialog.setTitle("Save a location to continue...");
                    alertDialog.setPositiveButton("ok", null);
                    alertDialog.setIcon(R.drawable.alerticon);
                    alertDialog.create().show();
                } else if (locCount.getText().toString().equals("Loading..."))
                    Toast.makeText(getApplicationContext(), "Loading... please wait", Toast.LENGTH_LONG).show();
                else {
                    startActivity(new Intent(HomepageActivity.this, FavLocListActivity.class));
                    finish();
                }
            }
        });

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (facebookUser)
                    Toast.makeText(getApplicationContext(), "FB Profile Image cannot be updated here!", Toast.LENGTH_LONG).show();
                else {
                    if (ContextCompat.checkSelfPermission(HomepageActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(HomepageActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            //Toast.makeText(getApplicationContext(),"Rationale",Toast.LENGTH_LONG).show();
                            alertDialog.setTitle("Requires permission");
                            alertDialog.setMessage("This app requires permission to read device storage to update profile picture");
                            alertDialog.setCancelable(false);
                            alertDialog.setPositiveButton("Ask me again", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(HomepageActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                                }
                            });
                            alertDialog.setNegativeButton("ok", null);
                            alertDialog.create().show();
                        } else {
                            ActivityCompat.requestPermissions(HomepageActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                        }
                    } else
                        selectPhotoFromGallery();
                }
            }
        });


        if (!networkIsAvailable()) {
            Toast.makeText(getApplicationContext(), "Please connect to Internet and try again!", Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(), "Please connect to Internet and try again!", Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(), "Please connect to Internet and try again!", Toast.LENGTH_LONG).show();
        }

        dbCountReference = FirebaseDatabase.getInstance().getReference().child(firebaseAuth.getCurrentUser().getUid()).child("Locations");
        dbCountReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren())
                    LocationCount++;
                if (LocationCount == 0)
                    locCount.setText(R.string.locationCountZero);
                else if (LocationCount == 1)
                    locCount.setText(R.string.locationCountOne);
                else
                    locCount.setText("You have saved " + LocationCount + " Favourite Locations");
                LocationCount = 0;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(HomepageActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                alertDialog.setTitle("Requires permission");
                alertDialog.setMessage("This app requires permission to access your device location");
                alertDialog.setCancelable(false);
                alertDialog.setPositiveButton("Ask me again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(HomepageActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 2000);
                    }
                });
                alertDialog.setNegativeButton("ok", null);
                alertDialog.create().show();
            } else {
                ActivityCompat.requestPermissions(HomepageActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_GRANTED);
            }
        }


        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String locationName = nameOfLocation.getText().toString().trim();
                String locationLandmark = landmarkOfLocation.getText().toString().trim();

                if (locationName.isEmpty()) {
                    nameOfLocation.setError("Required");
                    nameOfLocation.requestFocus();
                    return;
                }

                if (locationLandmark.isEmpty()) {
                    landmarkOfLocation.setError("Required");
                    landmarkOfLocation.requestFocus();
                    return;
                }

                alertDialog.setIcon(R.drawable.savelocationicon);
                alertDialog.setTitle("Capture Location");
                alertDialog.setMessage("Save this location?");
                alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveLocation();
                    }
                });
                alertDialog.setNegativeButton("Cancel", null);
                alertDialog.create().show();
            }
        });

        shareCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String locationName = nameOfLocation.getText().toString().trim();
                String locationLandmark = landmarkOfLocation.getText().toString().trim();

                if (locationName.isEmpty()) {
                    nameOfLocation.setError("Required");
                    nameOfLocation.requestFocus();
                    return;
                }

                Toast.makeText(getApplicationContext(), "Sharing location...", Toast.LENGTH_LONG).show();
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        "Sharing location from Favourite Locations \n \n" + "Location Name: " +
                                locationName + "\n" + "Landmark: " +
                                locationLandmark +
                                "\nLatitude: " + currentLat + "\n" + "Longitude: " + currentLng + "\n \n"
                                + "Click link below to navigate: \n"
                                + "https://www.google.co.in/maps/dir//" + currentLat + "," +
                                currentLng + "/@" + currentLat + "," + currentLng + ",17z");
                sendIntent.setType("text/plain");
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(sendIntent);
            }
        });
    }

    private void selectPhotoFromGallery() {
        try {
            if (profileImage.getDrawable().getConstantState() != getResources().getDrawable(R.drawable.profileicon).getConstantState()) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(HomepageActivity.this);
                String items[] = {"Choose from Gallery", "Remove Profile Image"};
                alertDialog.setTitle("Choose an option");
                alertDialog.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            Intent intent = new Intent(Intent.ACTION_PICK);
                            intent.setType("image/*");
                            startActivityForResult(intent, GALLERY_INTENT);
                        } else {
                            removeImageDialog.setTitle("Removing...");
                            removeImageDialog.setCancelable(false);
                            removeImageDialog.show();
                            StorageReference path = storageReference.child("Photos").child(firebaseAuth.getCurrentUser().getEmail());
                            path.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    removeImageDialog.dismiss();
                                    Toast.makeText(getApplicationContext(), "profile Image removed successfully!", Toast.LENGTH_LONG).show();
                                    profileImage.setImageResource(R.drawable.profileicon);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    removeImageDialog.dismiss();
                                    Toast.makeText(getApplicationContext(), "Please try again...", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
                AlertDialog ad = alertDialog.create();
                ad.show();
            } else {
                Toast.makeText(getApplicationContext(), "Opening Gallery...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, GALLERY_INTENT);
            }
        } catch (Exception e) {
            if (uploadCount > 2) {
                Toast.makeText(getApplicationContext(), "Please try again...", Toast.LENGTH_SHORT).show();
                DrawerLayout drawerlayout = (DrawerLayout) findViewById(R.id.drawerLayoutID);
                drawerlayout.closeDrawers();
            } else {
                Toast.makeText(getApplicationContext(), "Please wait...", Toast.LENGTH_SHORT).show();
                uploadCount++;
            }
        }
    }

    private void uploadProfileImage() {
        uploadImageDialog.setTitle("Uploading profile image...");
        uploadImageDialog.setCancelable(false);
        uploadImageDialog.show();
        StorageReference filePath = storageReference.child("Photos").child(firebaseAuth.getCurrentUser().getEmail());
        filePath.putFile(profileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        uploadImageDialog.dismiss();
                        UserProfileChangeRequest userProfile = new UserProfileChangeRequest.Builder()
                                .setPhotoUri(profileUri)
                                .build();
                        firebaseAuth.getCurrentUser().updateProfile(userProfile);
                        profileImage.setImageURI(profileUri);
                        Picasso.with(HomepageActivity.this)
                                .load(profileUri)
                                .memoryPolicy(MemoryPolicy.NO_CACHE)
                                .networkPolicy(NetworkPolicy.NO_CACHE)
                                .centerCrop()
                                .fit()
                                .into(profileImage);
                        Toast.makeText(getApplicationContext(), "Profile image upload successful!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        uploadImageDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Failed! Please try again", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean networkIsAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void saveLocation() {
        setMarker(currentLat, currentLng);
        marker.showInfoWindow();
        final String locationName = nameOfLocation.getText().toString().trim();
        final String locationLandmark = landmarkOfLocation.getText().toString().trim();
        final String locationLat = currentLat;
        final String locationLng = currentLng;

        /*//Toast.makeText(getApplicationContext(),"Latitude: " + currentLat + "\nLongitude: " + currentLng ,Toast.LENGTH_LONG).show();
        //below code was written to validate if duplicate record is being added. this feature can be given in further releases
        dbCountReference = FirebaseDatabase.getInstance().getReference().child(firebaseAuth.getCurrentUser().getUid()).child("Locations");

        dbCountReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()) {
                    if(locationLat.equals(ds.getValue(locData.class).getLocLat()) )
                            if(locationLng.equals(ds.getValue(locData.class).getLocLng()))
                    {
                        sameLocationCount++;
                        if (sameLocationCount > 1)
                        {
                            Log.d("DataSnapshot", ds.getValue(locData.class).getLocLat());
                            locationAlreadyPresentDialog.setTitle("Location exists")
                                    .setMessage("Details of existing location \n Name: " + ds.getValue(locData.class).getLocName()
                                            + "\n LandMark: " + ds.getValue(locData.class).getLocLandMark()
                                            + "\n Latitude: " + ds.getValue(locData.class).getLocLat()
                                            + "\n Longitude: " + ds.getValue(locData.class).getLocLng())
                                    .setIcon(R.drawable.alert)
                                    .setCancelable(true)
                                    .setPositiveButton("OK", null)
                                    .setNegativeButton("FavLocs List", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            startActivity(new Intent(HomepageActivity.this, FavLocListActivity.class));
                                            finish();
                                        }
                                    })
                                    .create().show();
                            locationExists = true;
                            return;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(),"Data Error. Please try again...", Toast.LENGTH_LONG).show();
            }
        });

        if (!locationExists){*/
        String id = databaseReference.push().getKey();
        locData data = new locData(locationName, locationLandmark, locationLat, locationLng, id);
        FirebaseUser user = firebaseAuth.getCurrentUser();
        databaseReference.child(user.getUid()).child("Locations").child(id).setValue(data);
        Toast.makeText(getApplicationContext(), "FavLoc saved successfully!", Toast.LENGTH_LONG).show();
        //}

    }

    private void setMarker(String currentLat, String currentLng) {
        if (marker != null)
            marker.remove();


        MarkerOptions options = new MarkerOptions()

                .title("FavLoc")
                .draggable(false)
                .position(new LatLng(Double.parseDouble(currentLat), Double.parseDouble(currentLng)))
                .snippet("snippet")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.markericon));

        marker = mGoogleMap.addMarker(options);
    }

    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragmentID);
        mapFragment.getMapAsync(this);
    }

    private void init() {
        saveButton = (Button) findViewById(R.id.hpcaptureButtonID);
        nameOfLocation = (EditText) findViewById(R.id.hpLocationTitleID);
        landmarkOfLocation = (EditText) findViewById(R.id.hpLandmarkID);
        locCount = (TextView) findViewById(R.id.hpDisplayCountID);
        shareCurrentLocation = (Button) findViewById(R.id.shareCurrentLocationID);

        firebaseAuth = FirebaseAuth.getInstance();
        loginManager = LoginManager.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        scanner = new ZXingScannerView(this);

        alertDialog = new AlertDialog.Builder(this);
        alertDialog2 = new AlertDialog.Builder(this);
        locationAlreadyPresentDialog = new AlertDialog.Builder(this);
        uploadImageDialog = removeImageDialog = new ProgressDialog(this);
        locationManager = (LocationManager) getSystemService(LocationManager.GPS_PROVIDER);

    }

    //below method is used to check if google play services is installed in the user's device.
    private boolean googlePlayServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS)
            return true;
        else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(getApplicationContext(), "Cannot connect to Google play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    //below method has to be overrided when onMapReadyCallback is implemented.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        //below code exracts user's location but we are doing it in googleclient's method
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);
        //goToLocationZoom(13.038622, 77.577576, 15);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();
        //Toast.makeText(getApplicationContext(),"Map Ready", Toast.LENGTH_LONG).show();
        if (mGoogleMap != null) {
            mGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    View v = getLayoutInflater().inflate(R.layout.info_window, null);

                    TextView iwName = (TextView) v.findViewById(R.id.IWnameID);
                    TextView iwLandmark = (TextView) v.findViewById(R.id.IWLandmarkID);

                    iwName.setText(nameOfLocation.getText().toString().trim());
                    iwLandmark.setText(landmarkOfLocation.getText().toString().trim());

                    return v;
                }
            });
        }
    }

    //below method takes camera to the specified lat long
    /*private void goToLocation(double i, double i1) {
        LatLng ll = new LatLng(i, i1);
        CameraUpdate update = CameraUpdateFactory.newLatLng(ll);
        mGoogleMap.moveCamera(update);
    }*/

    //below method takes camera to the specified lat long with zoom
    /*private void goToLocationZoom(double i, double i1, float zoom) {
        LatLng ll = new LatLng(i, i1);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
        mGoogleMap.moveCamera(update);
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.maps_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.mapTypeHybridID:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            case R.id.mapTypeTerrainID:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            case R.id.mapTypeNormalID:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case R.id.mapTypySatID:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            default:
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        currentLat = String.valueOf(location.getLatitude());
        currentLng = String.valueOf(location.getLongitude());

        LocationSettingsRequest.Builder checkGps = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        //Toast.makeText(getApplicationContext(),"On Connected", Toast.LENGTH_LONG).show();

        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, checkGps.build());

            if (result != null) {
                result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                    @Override
                    public void onResult(LocationSettingsResult locationSettingsResult) {
                        final Status status = locationSettingsResult.getStatus();

                        switch (status.getStatusCode()) {
                            case LocationSettingsStatusCodes.SUCCESS:
                                // All location settings are satisfied. The client can initialize location
                                // requests here.

                                break;
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied. But could be fixed by showing the user
                                // a optionsDialog.
                                try {
                                    // Show the optionsDialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    if (status.hasResolution()) {
                                        status.startResolutionForResult(HomepageActivity.this, 1000);
                                    }
                                } catch (IntentSender.SendIntentException e) {
                                    // Ignore the error.
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                // Location settings are not satisfied. However, we have no way to fix the
                                // settings so we won't show the optionsDialog.
                                break;
                        }
                    }
                });
            }

            checkGps.setAlwaysShow(true);
        } else
            {
            final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                buildAlertMessageNoGps();
            }

            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            boolean network_enabled = false;

            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) {
            }

            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ex) {
            }

            if (!gps_enabled && !network_enabled) {
                // notify user
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setMessage(getResources().getString(R.string.gps_network_not_enabled));
                dialog.setPositiveButton(getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        // TODO Auto-generated method stub
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                        //get gps
                    }
                });
                dialog.setNegativeButton(getString(R.string.Cancel), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        // TODO Auto-generated method stub

                    }
                });
                dialog.create().show();
            }
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This app requires GPS to be enabled.")
                .setCancelable(false)
                .setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        Toast.makeText(getApplicationContext(), "Enable GPS to save your current location!", Toast.LENGTH_LONG).show();
                        Toast.makeText(getApplicationContext(), "Enable GPS to save your current location!", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setIcon(R.drawable.alerticon);
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getApplicationContext(), "Connection Suspended", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null)
            Toast.makeText(getApplicationContext(), "Can't get current location", Toast.LENGTH_LONG).show();
        else {
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 15);
            mGoogleMap.animateCamera(update);
            currentLat = String.valueOf(location.getLatitude());
            currentLng = String.valueOf(location.getLongitude());
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout layout = (DrawerLayout) findViewById(R.id.drawerLayoutID);
        if(cameraOpen)
        {
            startActivity(new Intent(this, HomepageActivity.class));
            cameraOpen=false;
            finish();
            return;
        }
        if (layout.isDrawerOpen(GravityCompat.START))
                layout.closeDrawer(GravityCompat.START);
        else {
                alertDialog.setTitle("Exit Favourite locations?");
                alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HomepageActivity.this.finish();
                        Toast.makeText(getApplicationContext(), "Good Bye!", Toast.LENGTH_LONG).show();
                    }
                });
                alertDialog.setNegativeButton("Cancel", null);
                alertDialog.setIcon(R.drawable.exiticon);
                alertDialog.create().show();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayoutID);
        drawerLayout.closeDrawers();
        int id = item.getItemId();

        if (id == R.id.logoutID) {
            firebaseAuth.signOut();
            loginManager.logOut();
            startActivity(new Intent(HomepageActivity.this, LoginActivity.class));
            finish();
        }
       /* else if (id == R.id.saveManualFavLocsID) {
            startActivity(new Intent(HomepageActivity.this, ManualSaveActivity.class));
            finish();
        }*/
        else if (id == R.id.referFriendID)
        {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, ("Capture all your favourite locations under one roof." +
                    "\nShare/Navigate in just few clicks!" +
                    "\n\nDownload Favourite Locations app now!\n" +
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName())
                    + "\n\nTrailer here:\n" + Uri.parse("https://www.youtube.com/watch?v=ypOLFiONOpY")));
            sendIntent.setType("text/plain");
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(sendIntent);
        }
        else if (id == R.id.youtubeID) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/channel/UCrGvvyEEQnZEOyg_RwMdkrw")));
        }
        else if (id == R.id.contactUsID) {
            alertDialog2.setTitle("MaxTech, Bengaluru");
            alertDialog2.setMessage("In this fast-paced world, It is very important to keep yourself in par with upcoming/ongoing" +
                    " technologies. \n" + "\nMaxTech is a technical community which does the the job of assimilating content from " +
                    "various tech sites and always keep you up to date with maximum technology! \n");
            alertDialog2.setIcon(R.drawable.maxtechlogo);
            alertDialog2.setNegativeButton("Follow us", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent facebookAppIntent;
                    try {
                        facebookAppIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/981281535243682"));
                        startActivity(facebookAppIntent);
                    } catch (ActivityNotFoundException e) {
                        facebookAppIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://facebook.com/0MaxTech0"));
                        startActivity(facebookAppIntent);
                    }
                }
            });
            alertDialog2.setPositiveButton("Get in touch", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    StringBuilder body = new StringBuilder();
                    body.append("Hello MaxTech Team, \n \n");
                    body.append(" Please fill in your feedback/grievances  \n");
                    body.append("\n Regards, \n");
                    body.append(firebaseAuth.getCurrentUser().getDisplayName());
                    String company[] = {"0maxtech0@gmail.com"};
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "", null));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Favourite-Locations - " + firebaseAuth.getCurrentUser().getDisplayName() + " wants to cantact you");
                    intent.putExtra(Intent.EXTRA_EMAIL, company);
                    intent.putExtra(Intent.EXTRA_TEXT, body.toString());
                    startActivity(intent);
                }
            });
            alertDialog2.create().show();

        }
        else if (id == R.id.scanQRFavLoc)
        {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(HomepageActivity.this, Manifest.permission.CAMERA)) {
                    alertDialog.setTitle("Requires permission");
                    alertDialog.setMessage("This app requires permission to access your camera to scan QR Code");
                    alertDialog.setCancelable(false);
                    alertDialog.setPositiveButton("Ask me again", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(HomepageActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_GRANTED);
                        }
                    });
                    alertDialog.setNegativeButton("ok", null);
                    alertDialog.create().show();
                } else {
                    ActivityCompat.requestPermissions(HomepageActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_GRANTED);
                }
            }
            cameraOpen=true;
            setContentView(scanner);
            scanner.setResultHandler(this);
            scanner.startCamera();
        }
        else if (id == R.id.followUsID) {
            Intent facebookAppIntent;
            try {
                facebookAppIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/981281535243682"));
                startActivity(facebookAppIntent);
            } catch (ActivityNotFoundException e) {
                facebookAppIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://facebook.com/0MaxTech0"));
                startActivity(facebookAppIntent);
            }
        } else if (id == R.id.updatePasswordID) {
            startActivity(new Intent(HomepageActivity.this, UpdatePasswordActivity.class));
            finish();
        } else if (id == R.id.aboutAppID) {
            alertDialog2.setTitle("Favourite Locations");
            alertDialog2.setIcon(R.drawable.favlocicon);
            alertDialog2.setMessage("Capture all your favourite locations under one roof.\n" +
                    "Share/Navigate to any of them in just few clicks.\n" +
                    "Click on your favourite location to generate QR Code" +
                    " and scan the code from your friend's Favourite locations app to save!" +
                    "\nFor any queries, Feel free to get in touch with us!");
            alertDialog2.setPositiveButton("OK", null);
            alertDialog2.setNegativeButton(null, null);
            alertDialog2.create().show();
        } else if (id == R.id.rateUsID) {
            Uri uri = Uri.parse("market://details?id=" + getApplicationContext().getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            // To count with Play market backstack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName())));
            }
        } else {
            if (locCount.getText().toString().equals("You have not saved any favourite location yet")) {
                alertDialog.setTitle("Save a location to continue...");
                alertDialog.setPositiveButton("ok", null);
                alertDialog.setIcon(R.drawable.alerticon);
                alertDialog.create().show();
            } else if (locCount.getText().toString().equals("Loading..."))
                Toast.makeText(getApplicationContext(), "Loading... please wait", Toast.LENGTH_LONG).show();
            else {
                startActivity(new Intent(HomepageActivity.this, FavLocListActivity.class));
                finish();
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == GALLERY_INTENT) && (resultCode == RESULT_OK)) {
            profileUri = data.getData();
            uploadProfileImage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_GRANTED:
                {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    initMap();

                } else {

                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }
            case CAMERA_PERMISSION_GRANTED: {
                setContentView(scanner);
                scanner.setResultHandler(this);
                scanner.startCamera();
                break;
            }
        }

        // other 'case' lines to check for other
        // permissions this app might request
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanner.stopCamera();
    }

    @Override
    public void handleResult(Result result) {
        String resultText = result.getText();
        final String[] resultArray = resultText.split("\\\n");

        if (resultArray.length < 4)
        {
            Toast.makeText(getApplicationContext(),"Not a FavLoc QR Code! \nPlease scan valid code", Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(),"Not a FavLoc QR Code! \nPlease scan valid code", Toast.LENGTH_SHORT).show();
            onBackPressed();
            return;
        }

        AlertDialog.Builder qrScannedDialog = new AlertDialog.Builder(this);
        View mView = getLayoutInflater().inflate(R.layout.qr_scanned_dialog, null);

        TextView scannedName = (TextView) mView.findViewById(R.id.qrScannedName);
        TextView scannedLandMark = (TextView) mView.findViewById(R.id.qrScannedLandMark);
        TextView scannedLat = (TextView) mView.findViewById(R.id.qrScannedLat);
        TextView scannedLng = (TextView) mView.findViewById(R.id.qrScannedLng);
        Button scannedSave = (Button) mView.findViewById(R.id.qrScannedSaveButtonID);
        Button scannedEdit = (Button) mView.findViewById(R.id.qrScannedEditSaveButtonID);

        scannedName.setText(resultArray[0]);
        scannedLandMark.setText(resultArray[1]);
        scannedLat.setText(resultArray[2]);
        scannedLng.setText(resultArray[3]);

        scannedSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = databaseReference.push().getKey();
                locData data = new locData(resultArray[0], resultArray[1], resultArray[2], resultArray[3], id);
                FirebaseUser user = firebaseAuth.getCurrentUser();
                databaseReference.child(user.getUid()).child("Locations").child(id).setValue(data);
                Toast.makeText(getApplicationContext(),"Saved location successfully!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(HomepageActivity.this, FavLocListActivity.class));
                finish();
            }
        });

        scannedEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomepageActivity.this, ManualSaveActivity.class);
                i.putExtra("nameKey", resultArray[0]);
                i.putExtra("landMarkKey", resultArray[1]);
                i.putExtra("latKey", resultArray[2]);
                i.putExtra("lngKey", resultArray[3]);
                startActivity(i);
                finish();
            }
        });
        cameraOpen=false;
        qrScannedDialog.setView(mView);
        qrScannedDialog.create().show();
    }
}
