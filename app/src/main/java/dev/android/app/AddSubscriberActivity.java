package dev.android.app;

import android.content.pm.ActivityInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddSubscriberActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_subscriber);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        ((Button)findViewById(R.id.addSubscriberButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name=((TextView)findViewById(R.id.subscribersNameTextView)).getText().toString();
                final String phoneNumber=((TextView)findViewById(R.id.phoneNumberTextView)).getText().toString();
                final String noOfCoupans=((TextView)findViewById(R.id.noOfCoupansTextView)).getText().toString();
                final String validityDays=((TextView)findViewById(R.id.noOfDaysTextView)).getText().toString();

                if(name.isEmpty()||phoneNumber.isEmpty()||noOfCoupans.isEmpty()||validityDays.isEmpty()){
                    Toast.makeText(AddSubscriberActivity.this,"Enter all fields",Toast.LENGTH_SHORT).show();
                }
                else{
                    final DatabaseReference ownersReference=FirebaseDatabase.getInstance().getReference("owners");
                    String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
                    ownersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
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
                          count++;
                            ownersReference.child(uid).child("subscriptions_count").setValue(count);
                            ownersReference.child(uid).child("subscriptions").child(String.valueOf(count)).child("name").setValue(name);
                            ownersReference.child(uid).child("subscriptions").child(String.valueOf(count)).child("phone_number").setValue(phoneNumber);
                            ownersReference.child(uid).child("subscriptions").child(String.valueOf(count)).child("no_of_coupans").setValue(noOfCoupans);
                            ownersReference.child(uid).child("subscriptions").child(String.valueOf(count)).child("no_of_days").setValue(validityDays);
                            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                            ownersReference.child(uid).child("subscriptions").child(String.valueOf(count)).child("date_of_subscription").setValue(date);
                            Toast.makeText(AddSubscriberActivity.this,"Subscription added successfully",Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
                }
            }
        });
    }
}
