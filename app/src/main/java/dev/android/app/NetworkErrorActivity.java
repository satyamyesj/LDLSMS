package dev.android.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class NetworkErrorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_error);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

       Button reconnectButton = findViewById(R.id.reconnectButton);
        reconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInternetAvailable(NetworkErrorActivity.this)) {
                    Intent intentToStartTimelineActivity=new Intent(NetworkErrorActivity.this, TimelineActivity.class);
                    intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intentToStartTimelineActivity);
                } else {
                    Toast.makeText(NetworkErrorActivity.this, "you are still offline", Toast.LENGTH_SHORT).show();
                }
            }
        });
        final Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentToStartTimelineActivity = new Intent(NetworkErrorActivity.this, TimelineActivity.class);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intentToStartTimelineActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentToStartTimelineActivity.putExtra("Exit", true);
                startActivity(intentToStartTimelineActivity);
                finish();
                Toast.makeText(NetworkErrorActivity.this,"Press back button to exit",Toast.LENGTH_SHORT).show();
               TimelineActivity.exitFlag=true;
            }
        });
    }
    private boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }



    @Override
    public void onBackPressed() {
         if(TimelineActivity.exitFlag){
             super.onBackPressed();
             System.exit(0);
         }
         else if(!isInternetAvailable(this)){
             super.onBackPressed();
             Toast.makeText(this,"you are offline",Toast.LENGTH_SHORT).show();
             startActivity(new Intent(this,NetworkErrorActivity.class));
         }
         else if(isInternetAvailable(this)) {
             Toast.makeText(this,"reconnect to continue",Toast.LENGTH_SHORT).show();
         }
    }
}
