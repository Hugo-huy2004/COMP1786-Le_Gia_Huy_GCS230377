package com.example.mhike_cw_legiahuy.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.util.Prefs;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends BaseActivity {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null && Prefs.googleId(this) != null) {
            FirebaseHelper.setUserKey(Prefs.googleId(this));
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }
        mAuth.signOut();

        setContentView(R.layout.activity_login);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> signIn());
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String idToken = account == null ? null : account.getIdToken();
                if (idToken == null) {
                    fail("No ID token returned. Check default_web_client_id.");
                    return;
                }
                Prefs.setGoogleId(this, account.getId());
                FirebaseHelper.setUserKey(account.getId());
                firebaseAuthWithGoogle(idToken);
            } catch (ApiException e) {
                android.util.Log.e("MHike", "Google sign in failed", e);
                switch (e.getStatusCode()) {
                    case CommonStatusCodes.DEVELOPER_ERROR:
                        fail("Sign-in not configured. Check SHA-1 and google-services.json.");
                        break;
                    case CommonStatusCodes.SIGN_IN_REQUIRED:
                    case CommonStatusCodes.CANCELED:
                        break;
                    case CommonStatusCodes.NETWORK_ERROR:
                        fail("Network error. Check your connection.");
                        break;
                    default:
                        fail("Google sign in failed (code " + e.getStatusCode() + ")");
                }
            }
        }
    }

    private void fail(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        syncUser(user);
                    } else {
                        android.util.Log.e("MHike", "signInWithCredential failed", task.getException());
                        fail("Authentication failed: " + task.getException());
                    }
                });
    }

    private void syncUser(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;
        FirebaseHelper.profile().get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                goHome();
                return;
            }
            Map<String, Object> patch = new HashMap<>();
            patch.put("lastLoginAt", System.currentTimeMillis());
            if (!task.getResult().exists()) {
                patch.put("name", firebaseUser.getDisplayName());
                patch.put("email", firebaseUser.getEmail());
            }
            FirebaseHelper.profile().updateChildren(patch).addOnCompleteListener(t -> goHome());
        });
    }

    private void goHome() {
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
        finish();
    }
}
