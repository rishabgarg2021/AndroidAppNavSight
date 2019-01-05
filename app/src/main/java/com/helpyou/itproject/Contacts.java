package com.helpyou.itproject;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;


public class Contacts extends AppCompatActivity {

    private LocalContactsStorage localContactsStorage;

    //List of contacts from firebase containing phone numbers with Country codes
    private ArrayList<String> contactsFromFirebase;

    //List of contacts from the Contact list on the phone

    private ArrayList<Contact> contactsFromPhone = new ArrayList<>();

    //List of contacts with the name as well as the phone number they registered with
    private ArrayList<Contact> registeredContacts = new ArrayList<>();

    //List of contacts who can register with firebase
    private ArrayList<Contact> nonRegisteredContacts = new ArrayList<>();

    private Stack<Contact> alreadyInvitedContacts = new Stack<>();

    CustomContactAdapter customListAdapter;

    private DatabaseReference myRef;
    private String userId;
    private String myphone;
    private HashMap<String, String> usersUid = new HashMap<>();

    public Contacts() {
        contactsFromFirebase = new ArrayList<>();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contacts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_reload:
                loadContactsfromFirebase();
                return true;
            default:
                loadContactsfromFirebase();
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Wait for the permissions to be accepted

        boolean b = requestPermissions();
        String localContactsLocation = this.getApplicationContext().getFilesDir().getAbsolutePath();
        try {
            localContactsStorage = new LocalContactsStorage(localContactsLocation);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        Button btnRefresh = findViewById(R.id.action_reload);
        btnRefresh.setOnClickListener(v -> loadContactsfromFirebase());

        Intent i = getIntent();
        myphone = Objects.requireNonNull(Objects.requireNonNull(i.getExtras()).get("phone")).toString();
        userId = Objects.requireNonNull(i.getExtras().get("userId")).toString();

        Button btnBack = findViewById(R.id.btnContactsToMainMenuMap);
        btnBack.setOnClickListener(v -> finish());



        myRef = FirebaseDatabase.getInstance().getReference("users");
        try {
            registeredContacts = localContactsStorage.getLocalContacts();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        //checking if not able to get contacts form the local storage.
        if (registeredContacts == null) {
            registeredContacts = new ArrayList<>();
            loadContactsfromFirebase();
        } else {
            afterLoadingFromDataBase();
        }
    }

    //check permission needed for the the operation available.
    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(Contacts.this,
                Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (ActivityCompat.checkSelfPermission(Contacts.this,
                Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (ActivityCompat.checkSelfPermission(Contacts.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (ActivityCompat.checkSelfPermission(Contacts.this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return ActivityCompat.checkSelfPermission(Contacts.this,
                Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED;
    }

    //request for permission for the following.
    private boolean requestPermissions() {
        if (checkPermissions()) {
            return true;
        }
        ActivityCompat.requestPermissions(Contacts.this, new String[]
                {
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.SEND_SMS
                }, 1);

        return true;
    }


    //loading users form firebase
    private void loadContactsfromFirebase() {
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                registeredContacts.clear();
                nonRegisteredContacts.clear();
                contactsFromFirebase.clear();
                contactsFromPhone.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user == null || myphone == null) {
                        continue;
                    }

                    if (user.getPhoneNumber().equals(myphone)) {
                        userId = snapshot.getKey();
                    }
                    contactsFromFirebase.add(user.getPhoneNumber());
                    usersUid.put(user.getPhoneNumber(), snapshot.getKey());
                }
                boolean b = afterLoadingFromDataBase();
                try {
                    localContactsStorage.updateLocalContacts(registeredContacts);
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private boolean afterLoadingFromDataBase() {
        loadContactsFromPhone();
        try {
            filterContacts();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        //Now the ArrayLists registered and nonregistered contacts are filled up
        displayContactsInTheList();
        return true;
    }

    //displaying the contacts on the list
    private void displayContactsInTheList() {
        ListView contactListView = findViewById(R.id.contactListView);

        customListAdapter = new CustomContactAdapter();

        // Create ArrayAdapter using the planet list.
        final ArrayList<String> registered = getNames(registeredContacts);
        final ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, R.layout.contact_list_layout, registered);
        // Set the ArrayAdapter as the ListView's adapter.
        contactListView.setAdapter(customListAdapter);

        contactListView.setOnItemClickListener((adapterView, view, position, l) -> {
            Intent i = new Intent(Contacts.this, Chat.class);
            i.putExtra("chat_from_uid", userId);
            ArrayList<String> phoneNumber = registeredContacts.get(position).contactNumber;
            String phone = phoneNumber.get(0).replaceAll("\\s+", "");
            String userUID = registeredContacts.get(position).userId;
            i.putExtra("chat_to_uid", userUID);
            startActivity(i);
            finish();
        });
    }

    //filter the contacts by checking if the number rof present on both firebase and on the phone.
    private void filterContacts() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        //Now we have contacts from firebase and phone both
        Stack<String> contactsFromFirebaseWithoutCountryCode = new Stack<>();
        for (String contact : contactsFromFirebase) {
            contactsFromFirebaseWithoutCountryCode.push(stripOffTheCountryCode(contact));
        }
        for (Contact cont : contactsFromPhone) {
            Contact tempContact = new Contact(cont.contactName);
            for (String phoneNumber : cont.contactNumber) {
                String tempPhoneNum = stripOffTheCountryCode(phoneNumber);
                String phoneToAdd = deleteSpacesFromString(phoneNumber);
                if (contactsFromFirebaseWithoutCountryCode.contains(tempPhoneNum)) {
                    tempContact.contactNumber.add(phoneToAdd);
                    tempContact.setUserId(usersUid.get(phoneToAdd));
                    //Only uncomment below 3 lines if you see any problem with duplication
//                    if(isContactInTheList(tempContact.contactName, nonRegisteredContacts)){
//                        nonRegisteredContacts.remove(tempContact);
//                    }
                    registeredContacts.add(tempContact);
                    break;
                }
            }

            if (tempContact.contactNumber.size() == 0) {
                if (isContactInTheList(tempContact.contactName, registeredContacts)) {
                    if (isContactInTheList(tempContact.contactName, nonRegisteredContacts)) {
                        nonRegisteredContacts.add(cont);
                    }
                }
            }
        }

        for (Contact c : nonRegisteredContacts) {
        }
    }

    //helper method to check a contatc is in the list
    public boolean isContactInTheList(String name, ArrayList<Contact> contacts) {
        for (Contact contact : contacts) {
            if (contact.contactName.equals(name)) {
                return false;
            }
        }
        return true;
    }

    //loading all contacts from the phone
    private void loadContactsFromPhone() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursorAndroidContacts = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if ((cursorAndroidContacts != null ? cursorAndroidContacts.getCount() : 0) > 0) {
            while (cursorAndroidContacts.moveToNext()) {
                String id = cursorAndroidContacts.getString(
                        cursorAndroidContacts.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursorAndroidContacts.getString(cursorAndroidContacts.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));
                Contact contact = new Contact(name);
                //add the contacts if it's registered in database.
                if (cursorAndroidContacts.getInt(cursorAndroidContacts.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    assert pCur != null;
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        contact.contactNumber.add(phoneNo);
                    }
                    contactsFromPhone.add(contact);
                    //check here to add the contacts
                    pCur.close();
                }
            }
        }
        if (cursorAndroidContacts != null) {
            cursorAndroidContacts.close();
        }
    }

    //remove the country code
    private ArrayList<String> stripOffTheCountryCode(ArrayList<String> listNumbers) {
        ArrayList<String> tempListNumbers = new ArrayList<>();
        for (String number : listNumbers) {
            tempListNumbers.add(stripOffTheCountryCode(number));
        }
        return tempListNumbers;
    }

    //Only works with country codes with 2 digits for e.g. +61 or +91
    private String stripOffTheCountryCode(String phoneNumber) {
        String tempPhoneNumber = phoneNumber.replaceAll("\\s", "");
        if (phoneNumber.startsWith("+")) {
            tempPhoneNumber = tempPhoneNumber.substring(3);
        }
        return tempPhoneNumber;
    }

    //get the a array list of names froma array list of  contacts
    private ArrayList<String> getNames(ArrayList<Contact> contacts) {
        ArrayList<String> names = new ArrayList<>();
        for (Contact contact : contacts) {
            names.add(contact.contactName);
        }
        return names;
    }

    public void createActivity(String className) throws ClassNotFoundException {
        Class cls = Class.forName("com.helpyou.itproject." + className);
        Intent intent = new Intent(this, cls);
        intent.putExtra("phone", myphone);
        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }

    class CustomContactAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return registeredContacts.size() + nonRegisteredContacts.size();
        }

        @Override
        public Object getItem(int position) {

            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        //creating the list view
        @SuppressLint({"ViewHolder", "InflateParams"})
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = getLayoutInflater().inflate(R.layout.custom_contacts_layout, null);
            TextView textView_name = convertView.findViewById(R.id.textName);
            Button btn = convertView.findViewById(R.id.buttonInvite);
            TextView sentSymbol = convertView.findViewById(R.id.sent_symbol);
            if (registeredContacts.size() > position) {
                textView_name.setText(registeredContacts.get(position).contactName);
                btn.setVisibility(View.GONE);
                sentSymbol.setVisibility(View.GONE);
            } else {
                final int pos = position - registeredContacts.size();
                textView_name.setText(nonRegisteredContacts.get(pos).contactName);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onClickInvite(nonRegisteredContacts.get(pos));
                        alreadyInvitedContacts.add(nonRegisteredContacts.get(pos));
                        customListAdapter.notifyDataSetChanged();
                    }
                });
                if (alreadyInvitedContacts.contains(nonRegisteredContacts.get(pos))) {
                    btn.setVisibility(View.GONE);
                    sentSymbol.setVisibility(View.VISIBLE);
                } else {
                    sentSymbol.setVisibility(View.GONE);
                }
            }
            return convertView;
        }
    }

    //send message when invited
    private void onClickInvite(Contact contact) {
        SmsManager smsManager = SmsManager.getDefault();
        String phoneNum = deleteSpacesFromString(contact.contactNumber.get(0));
        String sms = "Please download HelpYou app from Android play store to help me with my commute!\n" +
                "https://ab46q.app.goo.gl/i/jzY86";
        smsManager.sendTextMessage(phoneNum, null, sms, null, null);
        Toast.makeText(this, "Invite Sent! to " + contact.contactName, Toast.LENGTH_SHORT).show();
    }


    public String deleteSpacesFromString(String s) {
        return s.replaceAll("\\s", "");
    }
}
