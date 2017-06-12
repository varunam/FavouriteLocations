package app.favloc.com.favouritelocations;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SignUpActivity extends AppCompatActivity {

    private EditText firstName, lastName, userName, password, dob, locality;
    private Button registerButton;
    private RadioGroup radioGroup;
    private RadioButton radioButton;

    private ProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseuser;
    private DatabaseReference databaseReference;

    //For date Picker
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        init();

        firstName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(firstName.getText().toString().isEmpty())
                    firstName.setError("Required");
            }
        });

        lastName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(lastName.getText().toString().isEmpty())
                    lastName.setError("Required");
            }
        });

        userName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(userName.getText().toString().isEmpty())
                    userName.setError("Required");
            }
        });

        password.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(password.getText().toString().isEmpty())
                    password.setError("Required");
            }
        });

        dob.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(dob.hasFocus())
                    new DatePickerDialog(SignUpActivity.this, listener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        locality.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(locality.getText().toString().isEmpty())
                    locality.setError("Required");
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

    }

    private void registerUser() {
        final String email = userName.getText().toString().trim();
        String Enteredpassword = password.getText().toString().trim();
        final String firstname = firstName.getText().toString().trim();
        final String dateofbirth = dob.getText().toString().trim();
        final String lastname = lastName.getText().toString().trim();
        final String localityAddress = locality.getText().toString().trim();
        int checkedButton = radioGroup.getCheckedRadioButtonId();
        radioButton = (RadioButton) findViewById(checkedButton);

        if (email.isEmpty() || Enteredpassword.isEmpty() || firstname.isEmpty() || lastname.isEmpty() || dateofbirth.isEmpty() || localityAddress.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please fill required fields", Toast.LENGTH_LONG).show();
            return;
        }

        if(radioGroup.getCheckedRadioButtonId()==-1)
        {
            Toast.makeText(getApplicationContext(),"Please select Gender",Toast.LENGTH_SHORT).show();
            return;
        }

        final String gender = radioButton.getText().toString().trim();

        if (dateofbirth.equals(new SimpleDateFormat("dd/MM/yyyy").format(new Date()))) {
            Toast.makeText(getApplicationContext(), "It cannot be today, right? \n Try Again", Toast.LENGTH_LONG).show();
            dob.requestFocus();
            return;
        }

        Date ddate = new Date();
        try {
            ddate = new SimpleDateFormat("dd/MM/yyyy").parse(dateofbirth);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (ddate.after(new Date())) {
            Toast.makeText(getApplicationContext(), "Don't tell me you time-travelled! \n Try again", Toast.LENGTH_LONG).show();
            dob.requestFocus();
            return;
        }

        progressDialog.setMessage("Creating Account...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, Enteredpassword)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful())
                        {
                            firebaseAuth.getCurrentUser().sendEmailVerification()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                progressDialog.dismiss();
                                                Toast.makeText(getApplicationContext(), "Verification email sent to " + email, Toast.LENGTH_LONG).show();
                                                saveUserInfo(firstname, lastname, dateofbirth, localityAddress, email, gender);
                                                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                                finish();
                                            } else {
                                                progressDialog.dismiss();
                                                Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                        }
                        else
                        {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(),task.getException().getMessage(),Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserInfo(String firstname, String lastname, String dateofbirth, String localityAddress, String email, String gender) {
        UserData userData = new UserData(firstname, lastname, dateofbirth, localityAddress, gender, email);
        UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName(firstname)
                .build();
        firebaseAuth.getCurrentUser().updateProfile(userProfileChangeRequest);
        databaseReference.child(firebaseAuth.getCurrentUser().getUid()).setValue(userData);

    }

    private void init() {
        firstName = (EditText) findViewById(R.id.SUfirstNameID);
        lastName = (EditText) findViewById(R.id.SUlastnameID);
        userName = (EditText) findViewById(R.id.SUusernameID);
        password = (EditText) findViewById(R.id.SUpasswordID);
        dob = (EditText) findViewById(R.id.SUDateID);
        locality = (EditText) findViewById(R.id.SUlocalityID);
        registerButton = (Button) findViewById(R.id.SUsignUpID);
        progressDialog = new ProgressDialog(this);
        radioGroup = (RadioGroup) findViewById(R.id.SUGenderID);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseuser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();

    }

    private DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth)
        {
            month++;
            String day,mnth;
            if(dayOfMonth<10)
                day = "0"+String.valueOf(dayOfMonth);
            else
                day = String.valueOf(dayOfMonth);

            if((month)<10)
                mnth="0"+String.valueOf(month);
            else
                mnth = String.valueOf(month);

            dob.setText(day+ "/" + mnth + "/" + year);

        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
        finish();
    }
}
