package com.helpyou.itproject;

/*
 * Author: Uvin Abeysinghe
 * Student Id : 789931
 * University of Melbourne
 *
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    //update the token on db when you get a new token
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("tokens");
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser() ;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());



        final String userId = sp.getString("userID", null);
        if(userId!=null){
            db.child(userId).child("device_token").setValue(s).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                }
            });
            Log.e("NEW_TOKEN",s);

        }



    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String notificationTitle = null;
        Object obj = remoteMessage.getData().get("title");
        if (obj != null) {
            notificationTitle = (obj.toString());
        }


        //help request notification
        if (notificationTitle.equals("Help")){

            String notificationMessage_temp=null;
            obj = remoteMessage.getData().get("body");
            if (obj != null) {
                notificationMessage_temp = (obj.toString());
            }
            final String notificationMessage=notificationMessage_temp;


            final DatabaseReference db = FirebaseDatabase.getInstance().getReference("help_req");
            final FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            final String myUid=currentFirebaseUser.getUid();

            //get all the info needed
            db.child(currentFirebaseUser.getUid()).child(notificationMessage).child("notified").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if((dataSnapshot.getValue() != null) && ((dataSnapshot.getValue(String.class)).equals("0"))){

                        db.child(currentFirebaseUser.getUid()).child(notificationMessage).child("notified").setValue("1").addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                DatabaseReference db2=db.child(currentFirebaseUser.getUid()).child(notificationMessage).child("from");
                                //getting from
                                db2.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        final String from = dataSnapshot.getValue(String.class);
                                        DatabaseReference db3 = FirebaseDatabase.getInstance().getReference("users");
                                        DatabaseReference db4 = db3.child(from).child("username");
                                        //getting name
                                        db4.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                final String name = dataSnapshot.getValue(String.class);
                                                DatabaseReference db5 = FirebaseDatabase.getInstance().getReference("help_req");
                                                DatabaseReference db6=db5.child(myUid).child(notificationMessage).child("message");
                                                //getting message
                                                db6.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                        String message = dataSnapshot.getValue(String.class);
                                                        helpNotification(name, myUid, from, message, notificationMessage);
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {

                                                    }
                                                });
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });

                            }
                        });




                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });





        //if its a message notification
        }else if(notificationTitle.equals("Message")){

            String userIdCode=null;
            obj = remoteMessage.getData().get("notiID");
            if (obj != null) {
                userIdCode = (obj.toString());
            }

            String pushId=null;
            obj = remoteMessage.getData().get("pushID");
            if (obj != null) {
                pushId = (obj.toString());
            }

            final String myUserIdCode = userIdCode;
            final String myPushId = pushId;


            //get all the information needed
            final DatabaseReference db = (FirebaseDatabase.getInstance().getReference("messages")).child(myUserIdCode).child(pushId);
            final FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            final String myUid=currentFirebaseUser.getUid();
            db.child("notify").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if((dataSnapshot.getValue().toString()).equals("1")){
                        db.child("type").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(((dataSnapshot.getValue()).toString()).equals("text")){
                                    db.child("from").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            final String fromUid = dataSnapshot.getValue().toString();
                                            db.child("message").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    final String message = dataSnapshot.getValue().toString();
                                                    (FirebaseDatabase.getInstance().getReference("users")).child(fromUid).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            String name = dataSnapshot.getValue().toString();
                                                            messageNotification(name, myUid, fromUid, message);
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });






        }


    }

    //create a message notification
    private void messageNotification(String name, String myUid, String toUid, String message){
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.app_icon)
                .setTicker("Hearty365")
                //     .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("New Message!")
                .setContentText(name + " : " + message)
                .setContentInfo("Click to see more!" );




        Intent newIntent= new Intent("UVIN2");
        newIntent.putExtra("chat_from_uid",myUid);
        newIntent.putExtra("chat_to_uid",toUid);


        PendingIntent newPendingIntent = PendingIntent.getActivity(this,0,newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(newPendingIntent);


        notificationManager.notify(/*notification id*/1, notificationBuilder.build());

    }




    //create a help request notification
    private void helpNotification(String name, String myUid,String helpUid, String message, String requestId ){

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.app_icon)
                .setTicker("Hearty365")
                //     .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("Help me?")
                .setContentText(name + " needs help")
                .setContentInfo("Click to see more!" );

        Intent newIntent= new Intent("UVIN");
        newIntent.putExtra("myUid",myUid);
        newIntent.putExtra("helpUid",helpUid);
        newIntent.putExtra("message",message);
        newIntent.putExtra("requestId",requestId);

        PendingIntent newPendingIntent = PendingIntent.getActivity(this,0,newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(newPendingIntent);


        notificationManager.notify(/*notification id*/1, notificationBuilder.build());

    }

}