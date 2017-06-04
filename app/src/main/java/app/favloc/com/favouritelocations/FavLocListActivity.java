package app.favloc.com.favouritelocations;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListViewCompat;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class FavLocListActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    private ListView favLocList;
    private LocListAdapter locListAdapter;
    ArrayList<HashMap<String, String>> locArrayList = new ArrayList<>();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_loc_list);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child(firebaseAuth.getCurrentUser().getUid()).child("Locations");
        favLocList = (ListView) findViewById(R.id.favLocListID);
        progressDialog = new ProgressDialog(this);

        if(favLocList.getCount()==0)
        {
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(progressDialog.isShowing())
                {
                    progressDialog.cancel();
                    Toast.makeText(getApplicationContext(),"Cannot connect. Please try again",Toast.LENGTH_LONG).show();
                    startActivity(new Intent(FavLocListActivity.this, HomepageActivity.class));
                    finish();
                }
            }
        },20000);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                locArrayList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren())
                {
                    HashMap<String, String> locItem = new HashMap<String, String>();
                    locItem.put("LocNameKey", ds.getValue(locData.class).getLocName());
                    locItem.put("LocLandMarkKey", ds.getValue(locData.class).getLocLandMark());
                    locItem.put("LatKey", ds.getValue(locData.class).getLocLat());
                    locItem.put("LngKey", ds.getValue(locData.class).getLocLng());
                    locItem.put("refKey", ds.getValue(locData.class).getRefKey());
                    locArrayList.add(locItem);
                }

                locListAdapter = new LocListAdapter(getApplicationContext(), locArrayList);
                favLocList.setAdapter(locListAdapter);

                if(favLocList.getCount()==0)
                {
                    startActivity(new Intent(FavLocListActivity.this, HomepageActivity.class));
                    finish();
                }
                if(progressDialog.isShowing() && favLocList.getCount()!=0)
                    progressDialog.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(FavLocListActivity.this, HomepageActivity.class));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            onBackPressed();
            return  true;
        }
        else
            return super.onOptionsItemSelected(item);
    }
}
