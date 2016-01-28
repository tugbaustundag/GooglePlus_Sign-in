package com.tugba.googlepluslogin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.*;
import com.google.android.gms.common.api.*;
import com.squareup.picasso.Picasso;

public class MainActivity  extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient mGoogleApiClient;
    private TextView mStatusTextView;
    private ProgressDialog mProgressDialog;
    private ImageView imgProfilePic;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Views
        mStatusTextView = (TextView) findViewById(R.id.status);
        imgProfilePic = (ImageView) findViewById(R.id.imgProfilePic);
        // Button listener tanımladım...
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);
        // Kullanıcının ID, email adres, ve basit profil bilgilerini alabilmek için sign-in(oturum açma) ayarı yapıyoruz
        // ID ve basit profil bilgileri DEFAULT_SIGN_IN içinde barınmaktadır
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .requestEmail()
                .build();
        //gso değişkeninde belirtilen seçeneklerle ve Google Sign-In API ile bağlantı kurmak için GoogleApiClient yapılandırdım.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }
    @Override
    public void onStart() {
        super.onStart();
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            //Kullanıcının önbelleğe kimlik bilgileri geçerli ise,  OptionalPendingResult tamamlancak ve
            //GoogleSignInResult anında kullanılabilir olacak.
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            //Kullanıcının daha önce telefonunda oturum açık değilse ya da  the otrum açma süresi dolmuş ise,
            //bu asekron çalısan bölüm kullanıcı girişini sağlıcaktır
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // GoogleSignInApi.getSignInIntent(...); döndüğü sonuc..
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }
    //Kullanıcın oturum açıp, açamadığını dönen metod
    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            // Oturum açma başarılı..
            GoogleSignInAccount acct = result.getSignInAccount();
           // Kullanıcının adını textview set ettik..
            mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
            // Kullanıcının resimin bulunduğu url alıp, bu resmi Picasso kütüphanesi ile imageview'de gösterdik
            Uri urldisplay =acct.getPhotoUrl();
            Picasso.with(MainActivity.this).load(String.valueOf(urldisplay)).into(imgProfilePic);

            //Kimlik denetimi yapıldığını göstermek için updateUI true  yapıldı.
            updateUI(true);
        } else {
            //Oturum açma başarısız...

            updateUI(false);
        }
    }
    //Oturum açılcağı zaman çalışan metod
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    //Oturum kapatılcağı zaman çalışan metod
       private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        updateUI(false);

                    }
                });
    }

    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        updateUI(false);
                    }
                });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);
            imgProfilePic.setImageDrawable(null);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;
        }
    }
}
