package com.proz.projectiot.GoogleOauth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.proz.projectiot.Common.CommonClass;
import com.proz.projectiot.R;
import com.proz.projectiot.SplashScreen;

public class GoogleOauth extends Activity {
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 100;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.google_oauth);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1044956637806-1el9i4pn4o1i7cdp6f6l787jg3f2lt3t.apps.googleusercontent.com")   // IMPORTANT
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> signIn());
     }
    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            Log.d("GOOGLE_AUTH", "Already signed in: " + account.getEmail());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }else{

        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // User info
            String name = account.getDisplayName();
            String email = account.getEmail();
            String idToken = account.getIdToken();
            Toast.makeText(getApplicationContext(),"Login Success",Toast.LENGTH_SHORT).show();
            CommonClass commonClass = new CommonClass();
            commonClass.putSharedPref(getApplicationContext(),"username",name);
            commonClass.putSharedPref(getApplicationContext(),"email",email);
            commonClass.putSharedPref(getApplicationContext(),"idToken",idToken);
            Intent intent= new Intent(getApplicationContext(), SplashScreen.class) ;
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
            Toast.makeText(getApplicationContext(),"Login Success",Toast.LENGTH_SHORT).show();
            Log.d("GOOGLE_AUTH", "User: " + name);
            Log.d("GOOGLE_AUTH", "Email: " + email);
            Log.d("GOOGLE_AUTH", "ID Token: " + idToken);

            // TODO: send idToken to your backend if needed

        } catch (ApiException e) {
            //Toast.makeText(getApplicationContext()," Sign in status "+e.getStatus()+" ",Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(),"status"+e.getStatusCode(),Toast.LENGTH_SHORT).show();
            Log.e("GOOGLE_AUTH", "Sign-in failed: " + e.getStatusCode());
        }
    }

    private void signOut() {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Toast.makeText(GoogleOauth.this, "Signed out", Toast.LENGTH_SHORT).show();
        });
    }
}
