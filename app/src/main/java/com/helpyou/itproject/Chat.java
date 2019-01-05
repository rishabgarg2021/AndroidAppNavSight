package com.helpyou.itproject;
import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import com.firebase.client.Firebase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.yavski.fabspeeddial.FabSpeedDial;
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter;


public class Chat extends BaseActivity implements SinchService.StartFailedListener {
    public static final int PICK_CALL = 2;
    private static final int GALLERY_PICK = 1;
    private static final int CAMERA_REQUEST_CODE = 3;
    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private final List<Messages> messagesList = new ArrayList<>();
    LinearLayout layout;
    ImageView sendButton;
    EditText messageArea;
    ScrollView scrollView;
    SimpleDateFormat sdf;
    private StorageReference mImageStorage;
    private String chatToUid = "";
    private String chatFromUid;
    private DatabaseReference reference1, reference2, myref;
    private Button callButton;
    private Button btnBack;
    private DatabaseReference imgDb;
    private String imgLink = "";
    private ArrayList<String> users = new ArrayList<>();
    private Button helpRequestBttn;
    private Uri imageUri;
    private RecyclerView mMessagesList;
    private SwipeRefreshLayout mRefreshLayout;
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;
    private int mCurrentPage = 1;
    //New Solution
    private int itemPos = 0;
    private String mLastKey = "";
    private String mPrevKey = "";
    private Call call;

    @Override
    public void onStart() {
        super.onStart();

    }

    //gets the image URL
    private void getImageUrl() {
        DatabaseReference profileUrl = imgDb.child(chatFromUid).child("imageUrl");

        profileUrl.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                imgLink = dataSnapshot.getValue().toString();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    /**
     * it helps to activate the ask for help button to be visible or invisible depending on the user connection.
     */
    private void checkAskForHelp() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("userConnect");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(chatFromUid) && dataSnapshot.hasChild(chatToUid)) {
                    if (dataSnapshot.child(chatFromUid).child("isConnected").getValue().toString().equals("1")) {
                        helpRequestBttn.setVisibility(View.INVISIBLE);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        sdf = new SimpleDateFormat("EEE, MMM d 'AT' HH:mm a");
        myref = FirebaseDatabase.getInstance().getReference("messages");
        Intent i = getIntent();
        chatFromUid = i.getExtras().get("chat_from_uid").toString();
        chatToUid = i.getExtras().get("chat_to_uid").toString();
        imgDb = FirebaseDatabase.getInstance().getReference("users");
        messageArea = (EditText) findViewById(R.id.messageArea);
        getImageUrl();


        mAdapter = new MessageAdapter(messagesList, getApplicationContext());
        mMessagesList = (RecyclerView) findViewById(R.id.messages_list);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.message_swipe_layout);

        mLinearLayout = new LinearLayoutManager(this);
        mMessagesList.setHasFixedSize(true);
        mMessagesList.setLayoutManager(mLinearLayout);
        mMessagesList.setAdapter(mAdapter);
        checkAskForHelp();
        //creates the fabSpeed Dial for user to send image either through camera or gallery.
        FabSpeedDial fabSpeedDial = (FabSpeedDial) findViewById(R.id.fab_speed_dial);
        fabSpeedDial.setMenuListener(new SimpleMenuListenerAdapter() {
            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {
                if (menuItem.toString().equals("Camera")) {
                    showCamera();
                }
                if (menuItem.toString().equals("Choose Media")) {
                    Intent galleryIntent = new Intent();
                    galleryIntent.setType("image/*");
                    galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);
                }
                return false;
            }
        });

        //asks the permission from user to record or make calls.
        if (ContextCompat.checkSelfPermission(Chat.this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                Chat.this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(Chat.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Chat.this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA},
                    1);
        }



        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurrentPage++;
                itemPos = 0;
                loadMoreMessages();
            }
        });
        sendButton = (ImageView) findViewById(R.id.sendButton);
        callButton = (Button) findViewById(R.id.callButton);
        btnBack = findViewById(R.id.buttonCancel);
        helpRequestBttn = findViewById(R.id.helpReqBttn);
        mImageStorage = FirebaseStorage.getInstance().getReference("chatImages");
        Firebase.setAndroidContext(this);
        reference1 = myref.child(chatFromUid + "_" + chatToUid);
        reference2 = myref.child(chatToUid + "_" + chatFromUid);
        loadMessages();

        //help request started
        android.content.Context context = this.getApplicationContext();
        helpRequestBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSinchServiceInterface().stopClient();
                Intent help = new Intent(Chat.this, HelpRequests.class);

                help.putExtra("message", "");
                help.putExtra("elderUid", chatFromUid);
                help.putExtra("helperUid", chatToUid);
                startActivity(help);
                finish();
            }
        });
        fakeData();
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //makes a call
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Map<String, String> headers = new HashMap<String, String>();
                if (chatToUid != "" && imgLink != "" && getSinchServiceInterface().isStarted()) {
                    headers.put("IMAGE_URL", imgLink);
                    headers.put("type", "chat");
                    Call call = getSinchServiceInterface().callUser(chatToUid, headers);
                    String callId = call.getCallId();
                    Intent callScreen = new Intent(Chat.this, CallScreenActivity.class);
                    callScreen.putExtra(SinchService.CALL_ID, callId);
                    startActivity(callScreen);
                }
            }
        });


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void fakeData() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("fake");
        ref.setValue(1).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                sendButton.performClick();
            }
        });

    }

    //load more messages when refreshed
    private void loadMoreMessages() {
        DatabaseReference messageRef = reference1;
        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(10);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages message = dataSnapshot.getValue(Messages.class);
                String messageKey = dataSnapshot.getKey();
                if (!mPrevKey.equals(messageKey)) {
                    messagesList.add(itemPos++, message);
                } else {
                    mPrevKey = mLastKey;
                }
                if (itemPos == 1) {
                    mLastKey = messageKey;
                }
                mAdapter.notifyDataSetChanged();
                mRefreshLayout.setRefreshing(false);
                mLinearLayout.scrollToPositionWithOffset(10, 0);
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //use camera to get images to send
    private void showCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageUri = getImageUri();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        }
    }

    //gets the image uri
    private Uri getImageUri() {
        Uri m_imgUri = null;
        File m_file;
        try {
            SimpleDateFormat m_sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

            String m_imagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg";
            m_file = new File(m_imagePath);
            m_imgUri = Uri.fromFile(m_file);
            m_imgUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".my.package.name.provider", m_file);

        } catch (Exception p_e) {
        }
        return m_imgUri;
    }

    private void loadMessages() {
        DatabaseReference messageRef = reference1;
        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages message = dataSnapshot.getValue(Messages.class);
                itemPos++;
                if (itemPos == 1) {
                    String messageKey = dataSnapshot.getKey();
                    mLastKey = messageKey;
                    mPrevKey = messageKey;
                }
                messagesList.add(message);
                users.add(message.getFrom());
                mAdapter.notifyDataSetChanged();
                mMessagesList.scrollToPosition(messagesList.size() - 1);
                mRefreshLayout.setRefreshing(false);
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    //sending the message
    private void sendMessage() {
        if (!getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient(chatFromUid);
        }
        String messageText = messageArea.getText().toString();
        if (!messageText.equals("")) {
            Map<String, Object> map = new HashMap<String, Object>();
            String currentDateandTime = sdf.format(new Date());
            map.put("message", messageText);
            map.put("user", chatToUid);
            map.put("from", chatFromUid);
            map.put("time", currentDateandTime);
            map.put("type", "text");
            map.put("notify", "0");
            reference1.push().setValue(map);
            map.put("notify", "1");
            reference2.push().setValue(map);
            messageArea.setText("");
        }

    }
   public void onDestroy() {
        if (getSinchServiceInterface() != null) {
            getSinchServiceInterface().stopClient();
        }
        super.onDestroy();
    }

    private String getFileExtension(Uri uri) {
        //gets the content of the file type.
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getMimeTypeFromExtension(cR.getType(uri));

    }

    //sending image messages
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CALL && resultCode == RESULT_OK) {
            call.answer();
            callButton.setText("Hang Up");

        }
        if (requestCode == PICK_CALL && resultCode == RESULT_CANCELED) {
            call.hangup();
            callButton.setText("CALL");

        }
        if ((requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) || (requestCode == GALLERY_PICK && resultCode == RESULT_OK)) {

            StorageReference reference;
            if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
                reference = mImageStorage.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));
            } else {
                imageUri = data.getData();
                reference = mImageStorage.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));
            }
            UploadTask upload = reference.putFile(imageUri);
            upload.addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(Chat.this, "Image could not be uploaded: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            //uploading the image
            upload.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return reference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        reference1 = myref.child(chatFromUid + "_" + chatToUid);
                        reference2 = myref.child(chatToUid + "_" + chatFromUid);
                        String url = task.getResult().toString();
                        Map<String, String> map = new HashMap<String, String>();
                        String currentDateandTime = sdf.format(new Date());
                        map.put("message", url);
                        map.put("user", chatFromUid);
                        map.put("from", chatFromUid);
                        map.put("time", currentDateandTime);
                        map.put("type", "image");
                        map.put("notify", "1");
                        reference1.push().setValue(map);
                        reference2.push().setValue(map);
                        messageArea.setText("");

                    }
                }
            });
        }
    }

    protected void onServiceConnected() {
        getSinchServiceInterface().setStartListener(this);

    }

    @Override
    public void onStartFailed(SinchError error) {
    }

    @Override
    public void onStarted() {

    }
}