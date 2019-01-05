package com.helpyou.itproject;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.stfalcon.frescoimageviewer.ImageViewer;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
/*
 * Authors:
 * Rish Garg       | ID:
 * Uvin Abeysinghe | ID: 789931
 * Tarnvir Grewal  | ID: 838527
 * University of Melbourne
 */

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;
    private DatabaseReference mUserDatabase;
    private List<Messages>  mMessageList;
    private ArrayList<String> users ;
    TextToSpeech mTts;
    private Context context;
    static final String TAG = "TTS";

    public MessageAdapter(List<Messages>  chats,Context context) {

        mMessageList = chats;
        this.context = context;
    }


    /**
     * assigns the layout to the viewof the message.
     * @param parent
     * @param viewType
     * @return
     */

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case VIEW_TYPE_ME:
                View viewChatMine = layoutInflater.inflate(R.layout.message_single_layout_right, parent, false);
                TextView text_out = viewChatMine.findViewById(R.id.message_text_layout_r);
                text_out.setBackgroundResource(R.drawable.text_out);
                viewHolder = new MyChatViewHolder(viewChatMine);

                break;
            case VIEW_TYPE_OTHER:
                View viewChatOther = layoutInflater.inflate(R.layout.message_single_layout, parent, false);
                TextView text_in = viewChatOther.findViewById(R.id.message_text_layout);
                text_in.setBackgroundResource(R.drawable.text_in);
                viewHolder = new OtherChatViewHolder(viewChatOther);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (TextUtils.equals(mMessageList.get(position).getFrom(),
                FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            configureMyChatViewHolder((MyChatViewHolder) holder, position);
        } else {
            configureOtherChatViewHolder((OtherChatViewHolder) holder, position);
        }
    }

    /**
     * here we can configure the view Holder.
     * we can get the type of the message and can show the media button if message type is not text.
     * has also attahced the listener to every text type message.
     * @param viewHolder holds the view with all the required information.
     */

    private void configureMyChatViewHolder(final MyChatViewHolder viewHolder, int i) {
        final  Messages c = mMessageList.get(i);
        final String from_user = mMessageList.get(i).getFrom();
        String message_type = c.getType();
        final ArrayList<String> images =new ArrayList<>();

        mUserDatabase = FirebaseDatabase.getInstance().getReference("users").child(from_user);

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String name = dataSnapshot.child("username").getValue().toString();
                String image = dataSnapshot.child("imageUrl").getValue().toString();


                viewHolder.displayName.setText(name);
                if(!image.isEmpty()) {
                    Picasso.with(viewHolder.profileImage.getContext()).load(image)
                            .placeholder(R.drawable.default_avatar).into(viewHolder.profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }


        });


        if(message_type.equals("text")) {

            viewHolder.imageButton.setVisibility(View.GONE);
            viewHolder.messageText.setVisibility(View.VISIBLE);
            viewHolder.messageText.setText(c.getMessage());
            viewHolder.dateTime.setText(c.getTime());
            viewHolder.speechButton.setVisibility(View.VISIBLE);
            viewHolder.speechButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listenText(c.getMessage());
                }
            });



        } else {
            viewHolder.messageText.setVisibility(View.GONE);
            viewHolder.imageButton.setVisibility(View.VISIBLE);
            images.add(c.getMessage());
            viewHolder.speechButton.setVisibility(View.INVISIBLE);
            viewHolder.imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!(c.getMessage().isEmpty())) {
                        new ImageViewer.Builder(v.getContext(), images)
                                .setStartPosition(0)
                                .show();
                    }

                }
            });


        }
    }
    //text to speech code
    private void listenText(String message){
        mTts = new TextToSpeech(context, new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTts.setLanguage(Locale.ENGLISH);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(context, "This language is not supported", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Log.v("TTS","onInit succeeded");
                        speak(message);
                    }
                } else {
                    Toast.makeText(context, "Initialization failed", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    void speak(String s){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Bundle bundle = new Bundle();
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            mTts.speak(s, TextToSpeech.QUEUE_FLUSH, bundle, null);
        } else {
            Log.v(TAG, "Speak old API");
            HashMap<String, String> param = new HashMap<>();
            param.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            mTts.speak(s, TextToSpeech.QUEUE_FLUSH, param);
        }
    }

    protected void onDestroy() {
        // Don't forget to shutdown tts!
        if (mTts != null) {
            Log.v(TAG,"onDestroy: shutdown TTS");
            mTts.stop();
            mTts.shutdown();
        }
    }

    private void configureOtherChatViewHolder(final OtherChatViewHolder viewHolder, final int i) {
        final Messages c = mMessageList.get(i);
        final ArrayList<String> images =new ArrayList<>();
        final String from_user = mMessageList.get(i).getFrom();
        String message_type = c.getType();


        mUserDatabase = FirebaseDatabase.getInstance().getReference("users").child(from_user);

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String name = dataSnapshot.child("username").getValue().toString();
                String image = dataSnapshot.child("imageUrl").getValue().toString();


                viewHolder.displayName.setText(name);

                if(!image.isEmpty()) {
                    Picasso.with(viewHolder.profileImage.getContext()).load(image)
                            .placeholder(R.drawable.default_avatar).into(viewHolder.profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }


        });

        //if its a text message, display this
        if(message_type.equals("text")) {
            viewHolder.imageButton.setVisibility(View.GONE);
            viewHolder.messageText.setVisibility(View.VISIBLE);
            viewHolder.messageText.setText(c.getMessage());
            viewHolder.dateTime.setText(c.getTime());
            viewHolder.speechButton.setVisibility(View.VISIBLE);

            viewHolder.speechButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listenText(c.getMessage());
                }
            });



        } else {
            //if image display this
            viewHolder.messageText.setVisibility(View.GONE);
            viewHolder.imageButton.setVisibility(View.VISIBLE);
            viewHolder.speechButton.setVisibility(View.INVISIBLE);

            images.add(c.getMessage());
            viewHolder.imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!(c.getMessage().isEmpty())) {
                        new ImageViewer.Builder(v.getContext(), images)
                                .setStartPosition(0)
                                .show();
                    }

                }
            });

        }

    }

    @Override
    public int getItemCount() {
        if (mMessageList!= null) {
            return mMessageList.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {

        if (TextUtils.equals(mMessageList.get(position).getFrom(),
                FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            return VIEW_TYPE_ME;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }

    /**
     *   created the view holders for chat from either ways
     *   the view holds the message content sent by the user.
     *   it has Message Text, profileImage, DisplayName of the message owner, imageButton to open the image sent
     *   dateTime the message is sent by the user from either way
     *   speechButton which translates the message to text.
     */
    private static class MyChatViewHolder extends RecyclerView.ViewHolder {

        public TextView messageText;
        public CircleImageView profileImage;
        public TextView displayName;
        public Button imageButton;
        public TextView dateTime;
        public Button speechButton;


        public MyChatViewHolder(View view) {
            super(view);
            messageText = (TextView) view.findViewById(R.id.message_text_layout_r);
            profileImage = (CircleImageView) view.findViewById(R.id.message_profile_layout);
            displayName = (TextView) view.findViewById(R.id.name_text_layout);
            dateTime=view.findViewById(R.id.time_text_layout);
            speechButton = view.findViewById(R.id.textToSpeechbtn);


            imageButton = view.findViewById(R.id.imageButton);
        }
    }

    private static class OtherChatViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;
        public CircleImageView profileImage;
        public TextView displayName;
        public Button imageButton;
        public TextView dateTime;
        public Button speechButton;
        public OtherChatViewHolder(View view) {
            super(view);
            messageText = (TextView) view.findViewById(R.id.message_text_layout);
            profileImage = (CircleImageView) view.findViewById(R.id.message_profile_layout);
            displayName = (TextView) view.findViewById(R.id.name_text_layout);
            dateTime=view.findViewById(R.id.time_text_layout);
            speechButton = view.findViewById(R.id.textToSpeechbtn);
            imageButton = view.findViewById(R.id.imageButton);
        }
    }
}