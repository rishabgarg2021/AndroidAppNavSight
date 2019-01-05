package com.helpyou.itproject;
import android.Manifest;
import android.app.Activity;
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

import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class ChatPop extends Activity {
    LinearLayout layout;
    ImageView sendButton;
    ImageButton imageButton;
    EditText messageArea;
    ScrollView scrollView;
    SimpleDateFormat sdf;
    private static final int GALLERY_PICK=1;
    private static final int CAMERA_REQUEST_CODE=3;
    public static final int PICK_CALL = 2;
    private StorageReference mImageStorage;
    private FirebaseAuth mAuth;
    private String chatToUid ="";
    private String chatFromUid;
    private DatabaseReference reference1,reference2,myref;
    private DatabaseReference imgDb;
    private ArrayList<String> users = new ArrayList<>();
    private Uri imageUri;
    private RecyclerView mMessagesList;
    private SwipeRefreshLayout mRefreshLayout;
    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;

    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;
    //New Solution
    private int itemPos = 0;

    private String mLastKey = "";
    private String mPrevKey = "";
    private ImageButton cameraButton;

    @Override
    public void onStart() {
        super.onStart();


    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_pop);
        sdf = new SimpleDateFormat("EEE, MMM d 'AT' HH:mm a");
        myref = FirebaseDatabase.getInstance().getReference("messages");
        Intent i = getIntent();
        LinearLayout ly = findViewById(R.id.layout);
        ly.getBackground().setAlpha(100);
        chatFromUid = i.getExtras().get("chat_from_uid").toString();
        chatToUid = i.getExtras().get("chat_to_uid").toString();
        imgDb = FirebaseDatabase.getInstance().getReference("users");
        messageArea = (EditText)findViewById(R.id.messageArea);
        Button cancelButton = findViewById(R.id.cancelButton);
        cameraButton = findViewById(R.id.cameraButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mAdapter = new MessageAdapter(messagesList,getApplicationContext());
        mMessagesList = (RecyclerView) findViewById(R.id.messages_list);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.message_swipe_layout);
        mLinearLayout = new LinearLayoutManager(this);
        mMessagesList.setHasFixedSize(true);
        mMessagesList.setLayoutManager(mLinearLayout);
        mMessagesList.setAdapter(mAdapter);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurrentPage++;
                itemPos = 0;
                loadMoreMessages();
            }
        });
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCamera();
            }
        });
        sendButton = (ImageView) findViewById(R.id.sendButton);
        imageButton = (ImageButton)findViewById(R.id.photoButton);
        mImageStorage= FirebaseStorage.getInstance().getReference("chatImages");
        Firebase.setAndroidContext(this);
        reference1= myref.child(chatFromUid + "_" + chatToUid);
        reference2= myref.child(chatToUid + "_" + chatFromUid);
        loadMessages();
        FirebaseDatabase.getInstance().getReference("userConnect").child(chatFromUid).child("isConnected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue().toString().equals("0")){
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });


        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent,"SELECT IMAGE"), GALLERY_PICK);

            }
        });
    }

    //use camere to get images for sending
    private void showCamera(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},1);
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageUri = getImageUri();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);


        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        }
    }

    //get the image uri
    private Uri getImageUri(){
        Uri m_imgUri = null;
        File m_file;
        try {
            SimpleDateFormat m_sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

            String m_imagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg";
            m_file = new File(m_imagePath);
            m_imgUri = Uri.fromFile(m_file);
            m_imgUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".my.package.name.provider",m_file);

        } catch (Exception p_e) {
        }
        return m_imgUri;
    }


    //loads more messages when pulled down
    private void loadMoreMessages() {

        DatabaseReference messageRef =reference1;
        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(10);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages message = dataSnapshot.getValue(Messages.class);
                String messageKey = dataSnapshot.getKey();
                if(!mPrevKey.equals(messageKey)){
                    messagesList.add(itemPos++, message);
                } else {
                    mPrevKey = mLastKey;
                }
                if(itemPos == 1) {
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

    //loads the messages
    private void loadMessages(){
        DatabaseReference messageRef =reference1;
        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages message = dataSnapshot.getValue(Messages.class);
                itemPos++;
                if(itemPos == 1){
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

    //sends the message
    private void sendMessage(){
        String messageText = messageArea.getText().toString();
        if(!messageText.equals("")){
            Map<String, Object> map = new HashMap<String,Object>();
            String currentDateandTime = sdf.format(new Date());
            map.put("message", messageText);
            map.put("user", chatToUid);
            map.put("from",chatFromUid);
            map.put("time", currentDateandTime);
            map.put("type", "text");
            map.put("notify", "0");
            reference1.push().setValue(map);
            map.put("notify", "1");
            reference2.push().setValue(map);
            messageArea.setText("");
        }
    }
    private String getFileExtension(Uri uri){
        //gets the content of the file type.
        ContentResolver cR= getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getMimeTypeFromExtension(cR.getType(uri));
    }



    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_CALL && resultCode == RESULT_OK){
            //call.addCallListener(new SinchCallListener());
        }
        if(requestCode==PICK_CALL && resultCode == RESULT_CANCELED){
        }
        if ((requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK)||(requestCode == GALLERY_PICK && resultCode == RESULT_OK) ){
            StorageReference reference;
            if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK){
                reference = mImageStorage.child(System.currentTimeMillis() + "."+getFileExtension(imageUri));
            }
            else{
                imageUri=data.getData();
                reference = mImageStorage.child(System.currentTimeMillis() + "."+getFileExtension(imageUri));
            }
            //uploading the image
            UploadTask upload = reference.putFile(imageUri);
            upload.addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ChatPop.this, "Image could not be uploaded: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });


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
                        map.put("from",chatFromUid);
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

}