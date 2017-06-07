package app.favloc.com.favouritelocations;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class UpdatePasswordActivity extends AppCompatActivity {

    private EditText currentPassword, newPassword, retypedNewPassword;
    private Button updateButton;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_password);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        currentPassword = (EditText) findViewById(R.id.UPcurrentPasswordID);
        newPassword = (EditText) findViewById(R.id.UPNewPassword);
        retypedNewPassword = (EditText) findViewById(R.id.UPconfirmPasswordID);
        updateButton = (Button) findViewById(R.id.UPResetPasswordID);

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating..");
        progressDialog.setCancelable(false);

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String curpass = currentPassword.getText().toString().trim();
                final String newpass = newPassword.getText().toString().trim();
                String retypepass = retypedNewPassword.getText().toString().trim();

                if (TextUtils.isEmpty(curpass)) {
                    currentPassword.setError("Required");
                    currentPassword.requestFocus();
                    return;
                }
                if (TextUtils.isEmpty(newpass)) {
                    newPassword.setError("Required");
                    newPassword.requestFocus();
                    return;
                }
                if (TextUtils.isEmpty(retypepass)) {
                    retypedNewPassword.setError("Required");
                    retypedNewPassword.requestFocus();
                    return;
                }
                if (!retypepass.equals(newpass)) {
                    retypedNewPassword.setError("Passwords do not match");
                    retypedNewPassword.requestFocus();
                    return;
                }
                if (curpass.equals(newpass)) {
                    newPassword.setError("New password cannot be same as old password");
                    newPassword.requestFocus();
                    return;
                }

                progressDialog.show();

                firebaseAuth.signInWithEmailAndPassword(firebaseAuth.getCurrentUser().getEmail(), curpass)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful())
                                {
                                    firebaseAuth.getCurrentUser().updatePassword(newpass)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        progressDialog.dismiss();
                                                        startActivity(new Intent(UpdatePasswordActivity.this, HomepageActivity.class));
                                                        finish();
                                                        Toast.makeText(getApplicationContext(), "Password Update Successful", Toast.LENGTH_SHORT).show();
                                                    }
                                                    else {
                                                        onFailure(task.getException());
                                                        newPassword.requestFocus();
                                                        progressDialog.dismiss();
                                                    }
                                                }

                                            });
                                }
                                else
                                {
                                    //Toast.makeText(getApplicationContext(),task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                    currentPassword.setError("Invalid old Password");
                                    currentPassword.requestFocus();
                                    return;
                                }
                            }
                        });


            }
        });
    }

    private void onFailure(Exception exception) {
        Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(UpdatePasswordActivity.this, HomepageActivity.class));
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
