package app.favloc.com.favouritelocations;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ManualSaveActivity extends AppCompatActivity {

    private EditText mslat, mslng, msname, mslandmark;
    private Button mscapture;

    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_save);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mslat = (EditText) findViewById(R.id.MSLatID);
        mslng = (EditText) findViewById(R.id.MSlngID);
        msname = (EditText) findViewById(R.id.MSnameID);
        mslandmark = (EditText) findViewById(R.id.MSlandmarkID);
        mscapture = (Button) findViewById(R.id.MSCaptureID);

            Intent i = getIntent();
            String name = i.getExtras().get("nameKey").toString();
            String landmark = i.getExtras().get("landMarkKey").toString();
            String lat = i.getExtras().get("latKey").toString();
            String lng = i.getExtras().getString("lngKey");

            msname.setText(name);
            mslandmark.setText(landmark);
            mslat.setText(lat);
            mslng.setText(lng);
            mslat.setEnabled(false);
            mslng.setEnabled(false);



        mscapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lat = mslat.getText().toString().trim();
                String lng = mslng.getText().toString().trim();
                String name = msname.getText().toString().trim();
                String landmark = mslandmark.getText().toString().trim();

                if (lat.isEmpty())
                {
                    mslat.setError("Required");
                    mslat.requestFocus();
                    return;
                }

                if (lng.isEmpty())
                {
                    mslng.setError("Required");
                    mslng.requestFocus();
                    return;
                }

                if (name.isEmpty())
                {
                    msname.setError("Required");
                    msname.requestFocus();
                    return;
                }

                if (landmark.isEmpty())
                {
                    mslandmark.setError("Required");
                    mslandmark.requestFocus();
                    return;
                }

                if (dotCount(lat, '.') > 1)
                {
                    mslat.setError("Doesn't exist on Earth!");
                    mslat.requestFocus();
                    return;
                }

                if (dotCount(lng, '.') > 1)
                {
                    mslng.setError("Doesn't exist on Earth!");
                    mslng.requestFocus();
                    return;
                }

                String id = databaseReference.push().getKey();
                locData data = new locData(name, landmark, lat, lng, id);
                FirebaseUser user = firebaseAuth.getCurrentUser();
                databaseReference.child(user.getUid()).child("Locations").child(id).setValue(data);
                Toast.makeText(getApplicationContext(),"Saved location successfully!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(ManualSaveActivity.this, FavLocListActivity.class));
                finish();
            }
        });
    }

    private int dotCount(String str, char c) {
        int count = 0;
        char[] strArray = str.toCharArray();
        for (char ch : strArray) {
            if (ch == c) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ManualSaveActivity.this, HomepageActivity.class));
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
