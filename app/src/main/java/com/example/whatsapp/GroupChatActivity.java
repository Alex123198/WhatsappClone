package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

public class GroupChatActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ImageButton SendMessageButton;
    private EditText userMessageInput;
    private ScrollView mScrollView;
    private TextView displayTextMessages;
    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef,GroupNameRef,GroupMessageKeyRef;
    private String currentGroupName,currentUserID,currentUserName,currentDate,currentTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        setContentView(R.layout.activity_group_chat);
        currentGroupName = getIntent().getExtras().get("groupName").toString();
        Toast.makeText(GroupChatActivity.this, currentGroupName, Toast.LENGTH_SHORT).show();
        GroupNameRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(currentGroupName);
        InitializeFields();
        GetUserInfo();
        SendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SendMessageInfoToDatabase();
                userMessageInput.setText("");
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        GroupNameRef.addChildEventListener(new ChildEventListener() { // Aici luam o referinta la groupul respectiv
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                if(dataSnapshot.exists()){ // si aici vedem daca exista conversatii pentru acel chat

                    DisplayMessages(dataSnapshot); // daca da le afisam
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void InitializeFields() {

        mToolbar = (Toolbar) findViewById(R.id.group_chat_bar_layout);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(currentGroupName);
        SendMessageButton = (ImageButton) findViewById(R.id.send_message_button);
        userMessageInput = (EditText) findViewById(R.id.input_group_message);
        displayTextMessages = (TextView) findViewById(R.id.group_chat_text_display);
        mScrollView = (ScrollView) findViewById(R.id.my_scroll_view);
    }

    public void GetUserInfo(){ // luam informatii despre user ca sa afisam detalii despre persoana care a trimis un mesaj

        UsersRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()){

                    currentUserName = dataSnapshot.child("name").getValue().toString(); // extragem numele acestuia
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendMessageInfoToDatabase() {

        String message = userMessageInput.getText().toString(); // luam inputul de la user
        String messageKey = GroupNameRef.push().getKey(); // grupul unde scrie
        if(TextUtils.isEmpty(message)){

            Toast.makeText(this, "Please write a message", Toast.LENGTH_SHORT).show(); // daca nu scrie nimic si vrea sa trimita mesaj nu este lasat si este rugat sa tasteze ceva
        }else{

            Calendar calForDate = Calendar.getInstance();
            SimpleDateFormat currentDateFormat = new SimpleDateFormat("dd/MM/yyyy"); // data cand a scris mesajul
            currentDate = currentDateFormat.format(calForDate.getTime());

            Calendar calForTime = Calendar.getInstance();
            SimpleDateFormat currentTimeFormat = new SimpleDateFormat("hh:mm a"); // ora cand a scris mesajul si minutele
            currentTime = currentTimeFormat.format(calForTime.getTime());

            HashMap<String,Object> groupMessageKey = new HashMap<>(); // fac un map unde stochez ce vreau sa fie afisat la trimiterea unui mesaj
            GroupNameRef.updateChildren(groupMessageKey);

            GroupMessageKeyRef = GroupNameRef.child(messageKey);
            HashMap<String,Object> messageInfoMap = new HashMap<>();
            messageInfoMap.put("name",currentUserName); // username
            messageInfoMap.put("message",message); // message
            messageInfoMap.put("date",currentDate);  // data
            messageInfoMap.put("time",currentTime); // timpul

            GroupMessageKeyRef.updateChildren(messageInfoMap); // si salvez mesajul in baza de date
        }
    }

    private void DisplayMessages(DataSnapshot dataSnapshot){ // partea asta doar afiseaza mesajele care is preluate din baza de date

        Iterator iterator = dataSnapshot.getChildren().iterator();
        while(iterator.hasNext()){ // parcurg cu un iterator mesajele din baza de date

            String chatDate = (String) ((DataSnapshot)iterator.next()).getValue(); //data
            String chatMeesage = (String) ((DataSnapshot)iterator.next()).getValue();//mesajul
            String chatName = (String) ((DataSnapshot)iterator.next()).getValue();//numele cui a scris
            String chatTime = (String) ((DataSnapshot)iterator.next()).getValue();// cand a scris (ora)
            displayTextMessages.append(chatName + " :\n" + chatMeesage + "\n" + chatTime // afisez datele pe ecran
            + "     " + chatDate + "\n\n");
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN); // daca cumva mesajele depasesc display-ul atunci dau scroll automat in jos
        }
    }
}
