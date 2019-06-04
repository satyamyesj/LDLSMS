package dev.android.app.Adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import dev.android.app.R;
import dev.android.app.TimelineActivity;
import dev.android.app.ViewSubscriptionsActivity;

public class Marker {
    private Context context;
    private String uid;
    private String iterator;
    private boolean generateAlertDialogAgain;
    public Marker(Context context,String uid, String iterator){
        this.context=context;
        this.uid=uid;
        this.iterator=iterator;
        generateAlertDialogAgain=true;
    }

    public  void showMarkEntry(){
        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_DayNight_Dialog);
        } else {
            context.setTheme(R.style.Theme_AppCompat_DayNight_Dialog);
            builder = new AlertDialog.Builder(context);
        }
        builder.setTitle("Marks entry ?")
                .setPositiveButton("mark", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        generateAlertDialogAgain= false;
                        final DatabaseReference ownersReference=FirebaseDatabase.getInstance().getReference("owners");
                        ownersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                int count = 0;
                                try {
                                    String countString = dataSnapshot.child(uid).child("subscriptions").child(iterator).child("no_of_coupans").getValue().toString();
                                    count = Integer.parseInt(countString);
                                }catch (NullPointerException e){
                                    count=0;
                                }
                                catch (NumberFormatException e){
                                    count=0;
                                }
                                count--;
                                ownersReference.child(uid).child("subscriptions").child(iterator).child("no_of_coupans").setValue(count);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                        Toast.makeText(context,"marked successfully",Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        generateAlertDialogAgain = false;
                        Toast.makeText(context,"did not marked",Toast.LENGTH_SHORT).show();
                        dialog.cancel();
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
    }
}
