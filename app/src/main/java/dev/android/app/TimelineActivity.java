package dev.android.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

import dev.android.app.Adapters.ViewAdapter;
import dev.android.app.FirebaseDataLoader.UploadActivity;

public class TimelineActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final int LOGIN_ACTIVITY_CALL = 1;
    private static final int REGISTRATION_ACTIVITY_CALL=2;
    static public boolean exitFlag = false;
    static private boolean generateAlertDialogAgain = true;

    private FirebaseAuth firebaseAuth;

    private DatabaseReference accountsReference = FirebaseDatabase.getInstance().getReference("accounts");
    private  DatabaseReference statisticsReference = FirebaseDatabase.getInstance().getReference("statistics");

    static public String uid=null;
    static public String account_user_name="USER_LOGGED_OUT";
    static public String account_type=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        //handling exit call from other activities
        if (getIntent().getBooleanExtra("Exit", false)) {
            Log.d("EXIT:", "Exit call");
            finish();
        }

        firebaseAuth = FirebaseAuth.getInstance();


        //checking for present logged in user
        if (firebaseAuth.getCurrentUser() != null) {
            TimelineActivity.uid = firebaseAuth.getCurrentUser().getUid();
            //checking for registration status of logged in user
            final ProgressDialog progressDialog = new ProgressDialog(TimelineActivity.this);
            progressDialog.setMessage("loading...");
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            accountsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (!snapshot.hasChild(firebaseAuth.getCurrentUser().getUid())) {
                        startActivityForResult(new Intent(TimelineActivity.this, RegisterActivity.class),REGISTRATION_ACTIVITY_CALL);
                    } else {
                        setDrawerLabels(TimelineActivity.this);
                        refreshTimeline();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
            progressDialog.dismiss();
        } else {
            showAuthenticationActivity();
        }
    }

    private void handleConnectivity() {
        if (!isInternetAvailable(this)) {
            startActivity(new Intent(this, NetworkErrorActivity.class));
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_ACTIVITY_CALL) {
                IdpResponse response = IdpResponse.fromResultIntent(data);
                // Successfully signed in
                if (resultCode == RESULT_OK) {
                    if (firebaseAuth.getCurrentUser() != null) {
                        Toast.makeText(TimelineActivity.this, "Verified  successfully", Toast.LENGTH_SHORT).show();
                        TimelineActivity.uid = firebaseAuth.getCurrentUser().getUid();
                        DatabaseReference accountsReference = FirebaseDatabase.getInstance().getReference("accounts");

                        final ProgressDialog progressDialog = new ProgressDialog(TimelineActivity.this);
                        progressDialog.setMessage("loading...");
                        progressDialog.setCancelable(false);
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.show();

                            accountsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.hasChild(firebaseAuth.getCurrentUser().getUid())) {
                                        startActivity(new Intent(TimelineActivity.this, RegisterActivity.class));
                                    } else {
                                        TimelineActivity.account_user_name = dataSnapshot.child(uid).child("name").getValue().toString();
                                        TimelineActivity.account_type = dataSnapshot.child(uid).child("account_type").getValue().toString();
                                        ((TextView) findViewById(R.id.nave_header_title)).setText(TimelineActivity.account_user_name);
                                        ((TextView) findViewById(R.id.nave_header_description)).setText("Mobile: " + firebaseAuth.getCurrentUser().getPhoneNumber());
                                        refreshTimeline();
                                    }
                                    progressDialog.dismiss();
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                    }
                    return;
                } else {
                    // Sign in failed
                    if (response == null) {
                        // User pressed back button
                        //Toast.makeText(TimelineActivity.this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                        final AlertDialog.Builder builder;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            builder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog);
                        } else {
                            this.setTheme(R.style.Theme_AppCompat_DayNight_Dialog);
                            builder = new AlertDialog.Builder(this);
                        }
                        builder.setTitle("Authentication cancelled")
                                .setMessage("Close App ?")
                                .setPositiveButton("sign in", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        TimelineActivity.generateAlertDialogAgain = false;
                                        dialog.cancel();
                                        showAuthenticationActivity();
                                    }
                                })
                                .setNegativeButton("close app", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        generateAlertDialogAgain = false;
                                        dialog.cancel();
                                        System.exit(0);
                                    }
                                })
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        if (generateAlertDialogAgain) {
                                            builder.show();
                                        }
                                    }
                                })
                                .show();
                        return;
                    }

                    if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                        Toast.makeText(TimelineActivity.this, "Network Error: authentication failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(TimelineActivity.this, "Unknown Error: authentication failed", Toast.LENGTH_SHORT).show();
                    //Log.e(TAG, "Sign-in error: ", response.getError());
                }
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    //Inflate Upper-Right Corner for diplaying selection list
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.timeline_activity, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.uploadMenuItem) {
            if (TimelineActivity.account_type.equals("OWNER")) {
                startActivity(new Intent(this, UploadActivity.class));
            } else {
                Toast.makeText(this, "only owner can upload menu", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.signOutItem) {
            TimelineActivity.this.signOut();
        }
        else if(id==R.id.refreshButton){
            refreshTimeline();
        }
        else if(id==R.id.addSubscriberButton){
            if (TimelineActivity.account_type.equals("OWNER")) {
               startActivity(new Intent(this, AddSubscriberActivity.class));
            } else {
                Toast.makeText(this, "only owner can add subscriber", Toast.LENGTH_SHORT).show();
            }
        }
        else if(id==R.id.viewSubscriptionsButton){
            DatabaseReference ownersReference=FirebaseDatabase.getInstance().getReference("owners");
            if (TimelineActivity.account_type.equals("OWNER")) {
                ownersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int count = 0;
                        try {
                            String countString = dataSnapshot.child(uid).child("subscriptions_count").getValue().toString();
                            count = Integer.parseInt(countString);
                        }catch (NullPointerException e){
                            count=0;
                        }
                        catch (NumberFormatException e){
                            count=0;
                        }
                        if(count>0) {
                            Intent intent=new Intent(TimelineActivity.this,ViewSubscriptionsActivity.class);
                           intent.putExtra("count",count);
                            startActivity(intent);
                        }else{
                            Toast.makeText(TimelineActivity.this,"subscriptions are unavailable",Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            } else {
             //   Toast.makeText(this, "only owner can upload menu", Toast.LENGTH_SHORT).show();
            }
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void signOut() {
        try {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(TimelineActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = getIntent();
                            finish();
                            startActivity(intent);
                        }
                    });
        }catch(Exception e){
            Toast.makeText(TimelineActivity.this,"error in sign out, consider restarting app",Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void showAuthenticationActivity() {
        List<AuthUI.IdpConfig> idpConfigList = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build()
        );
        AuthUI authuiInstance = AuthUI.getInstance();
        Intent authuiIntent = authuiInstance.createSignInIntentBuilder()
                .setAvailableProviders(idpConfigList)
                .setIsSmartLockEnabled(false)
                .setTheme(R.style.Theme_AppCompat_Light)
                .setLogo(AuthUI.NO_LOGO)
                .build();
        startActivityForResult(
                authuiIntent, LOGIN_ACTIVITY_CALL
        );
    }

    private void setRecyclerView(int count) {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) layoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        RecyclerView scrollView = findViewById(R.id.scrollView);
        scrollView.setLayoutManager(layoutManager);
        scrollView.setHasFixedSize(true);
        ViewAdapter viewAdapter = new ViewAdapter(this, count);
        scrollView.setAdapter(viewAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        generateAlertDialogAgain = true;
        handleConnectivity();
        if(!account_user_name.equals("USER_LOGGED_OUT")){
            refreshTimeline();
        }
    }

    private  void refreshTimeline(){
            statisticsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    int count = 0;
                    try {
                        String countString = dataSnapshot.child("owner_count").getValue().toString();
                        count = Integer.parseInt(countString);
                    }catch (NullPointerException e){
                        count=0;
                    }
                    catch (NumberFormatException e){
                        count=0;
                    }
                    if(count>0) {
                        setRecyclerView(count);
                    }else{
                        Toast.makeText(TimelineActivity.this,"menus are unavailable",Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }

    public void setDrawerLabels(final Context context){
        //fetch and diplay details in side pannel
        //retrieving account details

        accountsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String uid=firebaseAuth.getCurrentUser().getUid();
                try {
                    TimelineActivity.account_user_name = dataSnapshot.child(uid).child("name").getValue().toString();
                    TimelineActivity.account_type = dataSnapshot.child(uid).child("account_type").getValue().toString();
                    ((TextView) findViewById(R.id.nave_header_title)).setText(TimelineActivity.account_user_name);
                    ((TextView) findViewById(R.id.nave_header_description)).setText("Mobile: " + firebaseAuth.getCurrentUser().getPhoneNumber());
                }catch(Exception e){
                    Toast.makeText(context,"error, restart app",Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
