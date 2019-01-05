package com.helpyou.itproject;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;



import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hbb20.CountryCodePicker;

import java.util.ArrayList;
import java.util.Calendar;


public class userInfo extends AppCompatActivity {

    private static final int SELECT_PICTURE = 0;
    private ImageView imageView;
    private Uri profileImageUri;
    private  TextView percentageUpload;
    private  final String EXTRA_INFORMATION = "Other?";
    private String phoneNumberUser;
    private FirebaseAuth firebaseAuth;
    private Button saveButton,uploadImage,chooseImage;
    private ImageView dateOfBirthImage;
    private TextView dateOfBirthText;
    private EditText name,emergencyPhone;
    private ProgressBar imageBar;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabaseRef;
    CountryCodePicker ccp;
    private ImageButton medicalButton;
    private TextView medicalText ;
    private boolean imageUpload = false;
    String[] medical_conditions;
    boolean[] checked_medical_conditions;
    private ArrayList<Integer> mMedicalItems=new ArrayList<>();
    private ArrayList<String> userFinalMedicalCondition = new ArrayList<>();
    private String otherText = "";
    private String userId;
    private String uploadImageUrl="";
    private DatabaseReference db2 ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        dateOfBirthImage = findViewById(R.id.dateOfBirth);
        dateOfBirthImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialogTheme4 fragment = new DatePickerDialogTheme4();
                fragment.show(getSupportFragmentManager(),"Design 4");
            }
        });

        dateOfBirthText = findViewById(R.id.DOBtext);

        Intent i = getIntent();
        phoneNumberUser = i.getExtras().get("phone").toString();
        userId = i.getExtras().get("userId").toString();
        Toast.makeText(this,(phoneNumberUser+"dd"),Toast.LENGTH_SHORT).show();

        medicalText = findViewById(R.id.medicalText);
        uploadImage = findViewById(R.id.uploadImage);
        chooseImage = findViewById(R.id.chooseImage);
        imageBar= findViewById(R.id.progressBar);
        emergencyPhone = findViewById(R.id.emergencyPhone);
        imageView = findViewById(R.id.profileImage);
        firebaseAuth = FirebaseAuth.getInstance();
        name = findViewById(R.id.name);
        percentageUpload = findViewById(R.id.percentageUpload);
        saveButton = findViewById(R.id.saveButton);
        medicalButton= findViewById(R.id.medicalButton);
        medical_conditions = getResources().getStringArray(R.array.medical_condition);
        checked_medical_conditions = new boolean[medical_conditions.length];

        ccp = (CountryCodePicker)findViewById(R.id.ccp);
        ccp.registerCarrierNumberEditText(emergencyPhone);
        //all the images are stored under userImages.
        mStorageRef = FirebaseStorage.getInstance().getReference("userImages");

        mDatabaseRef = FirebaseDatabase.getInstance().getReference("users");


        uploadDataIfExists();
        //choose the image to get it display
        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImageChooser();
            }
        });
        //choose the image to upload in firebase and in user profile.
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage();
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                saveUserInfo();
            }
        });
        medicalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMedicalAlert();
            }
        });


    }
    //just upload the data if user id exist
    private void uploadDataIfExists(){

        DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");
        users.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(userId)){
                    DatabaseReference userObject = users.child(userId);
                    userObject.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            User user = dataSnapshot.getValue(User.class);
                            name.setText(user.getUsername());
                            dateOfBirthText.setText(user.getDateOfBirth());
                            if (user.getImageUrl() != "NO Image") {
                                uploadImageUrl = user.getImageUrl();
                                Glide.with(getApplicationContext()).load(user.getImageUrl()).into(imageView);

                            }


                            emergencyPhone.setText(user.getEmergencyPhone());
                            ArrayList<String> medicals = user.getMedicalCondition();
                            userFinalMedicalCondition=new ArrayList<String>();


                            for (int i = 0; i < medical_conditions.length; i++) {
                                if (medicals.contains(medical_conditions[i])) {
                                    userFinalMedicalCondition.add(medical_conditions[i]);
                                    checked_medical_conditions[i] = true;

                                }
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
    //medical options
    private void showMedicalAlert(){
        AlertDialog.Builder mBuilder  = new AlertDialog.Builder(userInfo.this);
        mBuilder.setTitle(R.string.dialog_title);
        //the items in the dialog and the checked items
        //add or remove the items
        mBuilder.setMultiChoiceItems(medical_conditions, checked_medical_conditions, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int position, boolean isChecked) {
                if(isChecked){
                    //check if the user has checked the item and then if the item is not included in the array list add or remove if there.
                    if(!mMedicalItems.contains(position)){
                        mMedicalItems.add(position);

                    }

                }



                else if (mMedicalItems.contains(position)) {

                    if(medical_conditions[position].equals(EXTRA_INFORMATION)){

                        otherText="";


                    }
                    mMedicalItems.remove(mMedicalItems.indexOf(position));

                }


            }
        });
        mBuilder.setCancelable(false);
        mBuilder.setPositiveButton(R.string.ok_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                userFinalMedicalCondition = new ArrayList<>();
                for(int i=0;i<mMedicalItems.size();i++){
                    userFinalMedicalCondition.add(medical_conditions[mMedicalItems.get(i)]);

                }
//                needs to add the text field to add the other context of the user.
                if(userFinalMedicalCondition.contains(EXTRA_INFORMATION)){
                    Log.v("Error1",otherText);
                    if(otherText.isEmpty()) {
                        Log.v("Error2","other Text is empty and is selected.");
                        AlertDialog.Builder builder = new AlertDialog.Builder(userInfo.this);
                        builder.setTitle("ExtraDetails");
                        final EditText input = new EditText(userInfo.this);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder.setView(input);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                otherText = input.getText().toString();
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder.setCancelable(false);
                        AlertDialog mDialog = builder.create();
                        mDialog.show();
                    }



                }

            }
        });
        mBuilder.setNegativeButton(R.string.dismiss_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        mBuilder.setNeutralButton(R.string.clearAll_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                for(int i=0;i<checked_medical_conditions.length;i++){
                    checked_medical_conditions[i]=false;
                    mMedicalItems.clear();
                }
            }
        });
        AlertDialog mDialog = mBuilder.create();
        mDialog.show();


    }
    //save the uer info
    private void saveUserInfo(){
        Log.v("name ",name.toString());
        if(name.getText().toString().isEmpty()){
            name.setError("Name required");
            name.requestFocus();
            return;

        }
        if(emergencyPhone.getText().toString().isEmpty()){
            emergencyPhone.setError("Emergency Number required");
            emergencyPhone.requestFocus();
            return;
        }

        User userInfo = null;
        //if the image has not been uploaded but the user has set the image, image will still be downloaded and saved to database.
        if(!imageUpload) {
            uploadImage();
        }


        userInfo   = new User(phoneNumberUser,name.getText().toString().trim(),ccp.getFullNumberWithPlus(),
                uploadImageUrl, dateOfBirthText.getText().toString(),userFinalMedicalCondition,"false");




        //Image is stored in storage and we need to store the info. of image(metaData) in
        // database.
        String userId = firebaseAuth.getUid();
        mDatabaseRef.child(userId).setValue(userInfo).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(userInfo.this,phoneNumberUser+"   User Profile Created",Toast.LENGTH_SHORT).show();

            }
        });
        Intent intent = new Intent(this,Main_Menu_Map.class);

        intent.putExtra("userId",userId);
        intent.putExtra("phone",phoneNumberUser);
        updateToken(userId);
        startActivity(intent);
        finish();




    }

    //get the uploaded file extention
    private String getFileExtension(Uri uri){
        //gets the content of the file type.
        ContentResolver cR= getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getMimeTypeFromExtension(cR.getType(uri));

    }
    //uploading the profile image
    private void uploadImage(){
        if(profileImageUri!=null){
            final StorageReference reference =  mStorageRef.child(System.currentTimeMillis()+"."+getFileExtension(profileImageUri));
            UploadTask upload = reference.putFile(profileImageUri);

            upload.addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(userInfo.this, "Image could not be uploaded: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    //upload is still progressing right now.
                    double progress = (100* taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                    percentageUpload.setText(Integer.toString((int)progress)+"%");

                    //user can still keep using app until progress is happening.
                    imageBar.setProgress((int)progress);

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
                        uploadImageUrl = task.getResult().toString();
                        imageUpload = true;
                        Toast.makeText(userInfo.this,"Upload Successfull", Toast.LENGTH_SHORT).show();




                    }
                }
            });
        }



    }

    //choosing the profile image
    private void showImageChooser(){

        Intent intent = new Intent();
        //ONLY SEE IMAGES
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);

    }

    //uploaidng the profile image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            profileImageUri = data.getData();
            Glide.with(getApplicationContext()).load(profileImageUri).into(imageView);

        }
    }


    //date of birth selecting
    public static class DatePickerDialogTheme4 extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int date = calendar.get(Calendar.DATE);
            DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                    this, year, month, date);
            return datePickerDialog;

        }


        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int date) {
            ((TextView) getActivity().findViewById(R.id.DOBtext)).setText(date + "/" + month  + "/" + year);
        }
    }


    //updating the forebase token on firebase
    private void updateToken(String mUserID) {

        final String userId = mUserID;
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {

                db2 = FirebaseDatabase.getInstance().getReference("tokens");
                String deviceToken = instanceIdResult.getToken();
                db2.child(userId).child("device_token").setValue(deviceToken).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        System.out.print("Token updated");
                    }
                });


            }
        });
    }


}








