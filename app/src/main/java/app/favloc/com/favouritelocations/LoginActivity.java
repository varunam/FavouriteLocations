package app.favloc.com.favouritelocations;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

public class LoginActivity extends AppCompatActivity {

    private EditText liuserName, lipassword;
    private Button lisignInButton;
    private LoginButton liFBLoginButton;
    private TextView licreateAccount, liforgotPassword;

    //callbackmanager to register callbacks from facebook.
    private CallbackManager callbackManager;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    //progress dialog to use wherever required
    private ProgressDialog progressDialog;
    private AlertDialog.Builder alertDialog, exitDialog;

    //TAG for logging
    private static String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //calling init function to initialize all the declared views.
        init();

        //logging in user if he is already signed in

        if(firebaseUser!=null)
        {
            for(UserInfo info : firebaseAuth.getCurrentUser().getProviderData()) {
                if (info.getProviderId().equals("facebook.com")) {
                    startActivity(new Intent(LoginActivity.this, HomepageActivity.class));
                    finish();
                } else if (firebaseUser.isEmailVerified()) {
                    startActivity(new Intent(LoginActivity.this, HomepageActivity.class));
                    finish();
                }
            }
        }
        //showing required field on focusing user name edit text
        liuserName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(liuserName.getText().toString().isEmpty())
                    liuserName.setError("Required");
            }
        });

        //showing required field on focusing password edit text
        lipassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(lipassword.getText().toString().isEmpty())
                    lipassword.setError("Required");
            }
        });

        //signing in on clicking continue with facebook
        liFBLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(getApplicationContext(),"Login attempt cancelled", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(getApplicationContext(),"Login attempt failed", Toast.LENGTH_LONG).show();
            }
        });

        //signing in on clicking sign in button.
        lisignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        //taking user to sign up page on clicking create account
        licreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
                finish();
            }
        });

        //taking user to forgot password page on clicking trouble signing in
        liforgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
                finish();
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken accessToken) {
        Log.d(TAG,"handleFacebookAccessToken " + accessToken);

        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful())
                        {
                            Toast.makeText(getApplicationContext(),"Logged in as " + firebaseAuth.getCurrentUser().getDisplayName() ,Toast.LENGTH_LONG).show();
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            updateUI(user);
                            startActivity(new Intent(LoginActivity.this, HomepageActivity.class));
                            finish();
                        }
                        else
                        {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        firebaseUser = user;
    }

    //method to initialize all the views declared
    private void init() {
        liuserName = (EditText) findViewById(R.id.LIusernameID);
        lipassword = (EditText) findViewById(R.id.LIpasswordID);
        lisignInButton = (Button) findViewById(R.id.LISignInButtonID);
        licreateAccount = (TextView) findViewById(R.id.LISignUpID);
        liforgotPassword = (TextView) findViewById(R.id.LIforgotPasswordID);
        liFBLoginButton = (LoginButton) findViewById(R.id.loginButtonFBID);

        callbackManager = CallbackManager.Factory.create();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        progressDialog = new ProgressDialog(this);
        exitDialog = new AlertDialog.Builder(this);
    }

    //method to handle activity result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    //method to handle user login
    private void loginUser() {
        String enteredUserName = liuserName.getText().toString().trim();
        String enteredPassword = lipassword.getText().toString().trim();
        if(enteredPassword.isEmpty() || enteredUserName.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Please fill required fields",Toast.LENGTH_LONG).show();
            return;
        }
        progressDialog.setMessage("Signing in...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        firebaseAuth.signInWithEmailAndPassword(enteredUserName, enteredPassword)
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if(task.isSuccessful()){
                            if(!firebaseAuth.getCurrentUser().isEmailVerified())
                            {
                                AlertDialog.Builder alertDialog = new AlertDialog.Builder(LoginActivity.this);
                                alertDialog.setTitle("Alert");
                                alertDialog.setMessage("Please verify your email to login");
                                alertDialog.setPositiveButton("Send mail again", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        firebaseAuth.getCurrentUser().sendEmailVerification()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if(task.isSuccessful())
                                                            Toast.makeText(getApplicationContext(),"Verification email sent to " + firebaseAuth.getCurrentUser().getEmail(), Toast.LENGTH_LONG).show();
                                                        else
                                                            Toast.makeText(getApplicationContext(),task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                    }
                                });
                                alertDialog.setNegativeButton("ok",null);
                                alertDialog.setCancelable(false);
                                AlertDialog ad = alertDialog.create();
                                ad.show();
                            }
                            else
                            {
                                Toast.makeText(getApplicationContext(),"Logged in Successfully!",Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, HomepageActivity.class));
                                finish();
                            }
                        }
                        else
                            Toast.makeText(getApplicationContext(),task.getException().getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        exitDialog.setTitle("Exit Favourite locations?");
        exitDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LoginActivity.this.finish();
                Toast.makeText(getApplicationContext(),"Good Bye!", Toast.LENGTH_LONG).show();
            }
        });
        exitDialog.setNegativeButton("Cancel", null);
        exitDialog.setIcon(R.drawable.exiticon);
        exitDialog.create().show();
    }

}
