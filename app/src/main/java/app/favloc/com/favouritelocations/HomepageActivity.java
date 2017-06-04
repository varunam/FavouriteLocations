package app.favloc.com.favouritelocations;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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
import com.google.android.gms.common.api.GoogleApiActivity;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
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
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

public class HomepageActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, NavigationView.OnNavigationItemSelectedListener {

    private Button saveButton;
    private EditText nameOfLocation, landmarkOfLocation;
    private TextView locCount;
    private ImageView profileImage;

    private FirebaseAuth firebaseAuth;
    private LoginManager loginManager;
    private DatabaseReference databaseReference, dbCountReference;
    private StorageReference storageReference;

    private GoogleMap mGoogleMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationManager locationManager;

    private AlertDialog.Builder alertDialog, alertDialog2;
    private ProgressDialog uploadImageDialog, removeImageDialog;
    private Uri profileUri;

    private String currentLat, currentLng;
    private static int LocationCount = 0, GALLERY_INTENT = 2, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 5;
    private int uploadCount = 0;
    private boolean facebookUser = false;
    private String facebookUserId, photoUri;

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
        for(UserInfo info: firebaseAuth.getCurrentUser().getProviderData())
        {
            if(info.getProviderId().equals("facebook.com"))
            {
                facebookUser = true;
                facebookUserId = info.getUid();
                photoUri = "https://graph.facebook.com/" + facebookUserId + "/picture?height=500";
            }
            else
                facebookUser = false;
        }

        if(facebookUser)
            Picasso.with(this).load(photoUri).into(profileImage);
        else
        {
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Photos").child(firebaseAuth.getCurrentUser().getEmail());
            try{
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
            }
            catch(Exception e)
            {
            }
        }
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(facebookUser)
                    Toast.makeText(getApplicationContext(),"FB Profile Image cannot be updated here!", Toast.LENGTH_LONG).show();
                else
                {
                    if(ContextCompat.checkSelfPermission(HomepageActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
                        if(ActivityCompat.shouldShowRequestPermissionRationale(HomepageActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE))
                        {
                            //Toast.makeText(getApplicationContext(),"Rationale",Toast.LENGTH_LONG).show();
                            alertDialog.setTitle("Requires permission");
                            alertDialog.setMessage("This app requires permission to read device storage to update profile picture");
                            alertDialog.setCancelable(false);
                            alertDialog.setPositiveButton("Ask me again", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(HomepageActivity.this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                                }
                            });
                            alertDialog.setNegativeButton("ok",null);
                            alertDialog.create().show();
                        }
                        else
                        {
                            ActivityCompat.requestPermissions(HomepageActivity.this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                        }
                    }
                    else
                    {
                        selectPhotoFromGallery();
                    }
                }
            }
        });

        if(!networkIsAvailable())
        {
            Toast.makeText(getApplicationContext(),"Please connect to Internet and try again!", Toast.LENGTH_LONG).show();
        }

        dbCountReference = FirebaseDatabase.getInstance().getReference().child(firebaseAuth.getCurrentUser().getUid()).child("Locations");
        dbCountReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren())
                    LocationCount++;
                if(LocationCount==0)
                    locCount.setText(R.string.locationCountZero);
                else if (LocationCount == 1)
                    locCount.setText(R.string.locationCountOne);
                else
                    locCount.setText("You have saved " + LocationCount + " Favourite Locations");
                LocationCount=0;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(HomepageActivity.this, Manifest.permission.ACCESS_FINE_LOCATION))
            {
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
            }
            else {
                ActivityCompat.requestPermissions(HomepageActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 2000);
            }
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String locationName = nameOfLocation.getText().toString().trim();
                String locationLandmark = landmarkOfLocation.getText().toString().trim();

                if(locationLandmark.isEmpty())
                {
                    nameOfLocation.setError("Required");
                    return;
                }
                if(locationName.isEmpty())
                {
                    landmarkOfLocation.setError("Required");
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

    }

    private void selectPhotoFromGallery() {
        try
        {
            if(profileImage.getDrawable().getConstantState() != getResources().getDrawable(R.drawable.profileicon).getConstantState()) {
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
                                    Toast.makeText(getApplicationContext(),"profile Image removed successfully!", Toast.LENGTH_LONG).show();
                                    profileImage.setImageResource(R.drawable.profileicon);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    removeImageDialog.dismiss();
                                    Toast.makeText(getApplicationContext(),"Please try again...", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
                AlertDialog ad = alertDialog.create();
                ad.show();
            }
            else
            {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, GALLERY_INTENT);
            }
        }
        catch (Exception e)
        {
            if(uploadCount>2)
            {
                Toast.makeText(getApplicationContext(),"Please try again...", Toast.LENGTH_SHORT).show();
                DrawerLayout drawerlayout = (DrawerLayout) findViewById(R.id.drawerLayoutID);
                drawerlayout.closeDrawers();
            }
            else{
                Toast.makeText(getApplicationContext(),"Please wait...", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getApplicationContext(),"Profile image upload successful!",Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        uploadImageDialog.dismiss();
                        Toast.makeText(getApplicationContext(),"Failed! Please try again",Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean networkIsAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void saveLocation() {
        String locationName = nameOfLocation.getText().toString().trim();
        String locationLandmark = landmarkOfLocation.getText().toString().trim();
        String locationLat = currentLat;
        String locationLng = currentLng;

        //Toast.makeText(getApplicationContext(),"Latitude: " + currentLat + "\nLongitude: " + currentLng ,Toast.LENGTH_LONG).show();
        String id = databaseReference.push().getKey();
        locData data = new locData(locationName, locationLandmark, locationLat, locationLng, id);
        FirebaseUser user = firebaseAuth.getCurrentUser();
        databaseReference.child(user.getUid()).child("Locations").child(id).setValue(data);
        Toast.makeText(getApplicationContext(),"Saved location successfully!", Toast.LENGTH_LONG).show();
        startActivity(new Intent(HomepageActivity.this, FavLocListActivity.class));
        finish();
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

        firebaseAuth = FirebaseAuth.getInstance();
        loginManager = LoginManager.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        alertDialog = new AlertDialog.Builder(this);
        alertDialog2 = new AlertDialog.Builder(this);
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
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getApplicationContext(),"Connection Suspended", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(),"Connection Failed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        if( location == null)
            Toast.makeText(getApplicationContext(),"Can't get current location", Toast.LENGTH_LONG).show();
        else{
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 15);
            mGoogleMap.animateCamera(update);
            currentLat = String.valueOf(location.getLatitude());
            currentLng = String.valueOf(location.getLongitude());
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout layout = (DrawerLayout)findViewById(R.id.drawerLayoutID);
        if (layout.isDrawerOpen(GravityCompat.START))
            layout.closeDrawer(GravityCompat.START);
        else
            {
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

        if(id == R.id.logoutID)
        {
            firebaseAuth.signOut();
            loginManager.logOut();
            startActivity(new Intent(HomepageActivity.this, LoginActivity.class));
            finish();
        }
        else if (id == R.id.contactUsID)
        {
            alertDialog2.setTitle("MaxTech, Bengaluru");
            alertDialog2.setMessage("Varun Gupta \nPh: 9886364759");
            alertDialog2.setNegativeButton("OK", null);
            alertDialog2.setPositiveButton("Send Email", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    StringBuilder body = new StringBuilder();
                    body.append("Hello MaxTech Team, \n \n");
                    body.append(" Please fill in your feedback/grievances  \n");
                    body.append("\n Regards, \n");
                    body.append(firebaseAuth.getCurrentUser().getDisplayName());
                    String developers[] = {"varuvgnc@gmail.com"};
                    String company[] = {"0maxtech0@gmail.com"};

                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto","",null));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Favourite-Locations - " + firebaseAuth.getCurrentUser().getDisplayName()+" wants to cantact you");
                    intent.putExtra(Intent.EXTRA_EMAIL, developers);
                    intent.putExtra(Intent.EXTRA_CC, company);
                    intent.putExtra(Intent.EXTRA_TEXT, body.toString());
                    startActivity(intent);
                }
            });
            alertDialog2.setIcon(R.drawable.developericon);
            alertDialog2.create().show();

        }
        else if (id == R.id.followUsID)
        {
            /*try {
                getApplicationContext().getPackageManager().getPackageInfo("com.facebook.katana", 0);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/FB+BG9n26VQ")));
            } catch (Exception e) {*/
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/0maxtech0")));
            //}
        }
        else if (id == R.id.aboutAppID)
        {
            alertDialog2.setTitle("Favourite Locations");
            alertDialog2.setIcon(R.drawable.favlocicon);
            alertDialog2.setMessage("FavLoc simplifies the way you are bookmarking your favourite locations!\n" +
                    "The app helps to capture, share or navigate to any of your favourite locations\n" +
                    "The app has been developed by MaxTech, Bengaluru \n" +
                    "Contact us to learn \"Android App Development\" \n");
            alertDialog2.setPositiveButton("OK", null);
            alertDialog2.create().show();
        }
        else if (id==R.id.rateUsID)
        {
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
        }
        else
        {
            if(locCount.getText().toString().equals("You have not saved any favourite location yet"))
            {
                alertDialog.setTitle("Save a location to continue...");
                alertDialog.setPositiveButton("ok", null);
                alertDialog.setIcon(R.drawable.alerticon);
                alertDialog.create().show();
            }
            else if(locCount.getText().toString().equals("Loading..."))
                Toast.makeText(getApplicationContext(),"Loading... please wait", Toast.LENGTH_LONG).show();
            else
            {
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
}
