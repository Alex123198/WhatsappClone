package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import java.util.HashMap;
import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button UpdateAccountSettings;
    private EditText userName,userStatus;
    private CircleImageView userProfileImage;
    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private static final int GalleryPick=1;
    private StorageReference UserProfileImagesRef;
    private ProgressDialog loadingBar;
    private Toolbar SettingsToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UserProfileImagesRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        InitializeFields();
        userName.setVisibility(View.INVISIBLE);

        UpdateAccountSettings.setOnClickListener(new View.OnClickListener() { // O data ce user-ul a apasat pe butonul de update se apeleaza "Update Settings"

            @Override
            public void onClick(View view){

                UpdateSettings();
            }
        });

        RetrieveUserInfo(); // Cand se intra in activity prin aceasta functie retrag informatiile din database pentru user-ul respectiv
        userProfileImage.setOnClickListener(new View.OnClickListener() { // Acetsa este "Circle Image View" unde se initializeaza poza de profil
            @Override
            public void onClick(View v) { // Aceasta parte extrage imaginea de profil din memoria telefonului

                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,GalleryPick);
            }
        });
    }
    private void InitializeFields() {

        UpdateAccountSettings = (Button) findViewById(R.id.update_settings_button);
        userName = (EditText) findViewById(R.id.set_user_name);
        userStatus = (EditText) findViewById(R.id.set_profile_status);
        userProfileImage = (CircleImageView) findViewById(R.id.set_profile_image);
        loadingBar = new ProgressDialog(this);
        SettingsToolBar = (Toolbar) findViewById(R.id.settings_toolbar);
        setSupportActionBar(SettingsToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");
    }

    private void UpdateSettings() {
        final StorageReference filePath = UserProfileImagesRef.child(currentUserID + ".jpg"); // preiau imaginea de profil din baza de date
        filePath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                final String setUserName = userName.getText().toString(); // iau username-ul
                String setStatus = userStatus.getText().toString(); // iau statusul
                String setImage = task.getResult().toString(); // iau si imaginea
                if(TextUtils.isEmpty(setUserName)){ // in caz ca nu completat partea cu username

                    Toast.makeText(SettingsActivity.this,"Please write your username first..",Toast.LENGTH_LONG).show();
                }
                else
                if(TextUtils.isEmpty(setStatus)){ // in caz ca nu a completat partea cu status

                    Toast.makeText(SettingsActivity.this,"Please fill in your status",Toast.LENGTH_SHORT).show();

                }
                else{

                    HashMap<String,Object> profileMap = new HashMap<>(); // creez un hashmap unde pun imagea de profil,numele,status si id-ul sau
                    profileMap.put("uid",currentUserID);
                    profileMap.put("name",setUserName);
                    profileMap.put("status",setStatus);
                    profileMap.put("image",setImage);
                    RootRef.child("Users").child(currentUserID).updateChildren(profileMap) // dupa care le incarc inapoi pentru user-ul corespunzator in caz ca isi da update la status sau imagine
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if (task.isSuccessful()){

                                        SendUserToMainActivity(); // in caz ca totul e ok va primi un mesaj pe ecran cum ca profilul a fost updatat cu succes si va fi trimis la activitatea principala
                                        Toast.makeText(SettingsActivity.this,"Profile Updated Succesfully",Toast.LENGTH_SHORT).show();
                                    }
                                    else{

                                        String message = task.getException().toString();
                                        Toast.makeText(SettingsActivity.this,"Error : " + message,Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { // In partea aceasta de cod se executa partea de preluare a imaginii din memoria locala a telefonului

        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==GalleryPick && resultCode==RESULT_OK && data!=null){

            Uri imageUri = data.getData();
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON) // Aici dau crop la imagine
                    .setAspectRatio(1,1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if(resultCode==RESULT_OK){

                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait,your profile image is updating");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();
                Uri resultUri = result.getUri();
                final StorageReference filePath = UserProfileImagesRef.child(currentUserID + ".jpg");               // In partea aceasta iau referinta catre imaginea de profil al user-ului curent
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                        if(task.isSuccessful()){    // Daca totul e ok imaginea este decupata si pusa in baza de date

                            Toast.makeText(SettingsActivity.this, "Profile image has been uploaded succesfully", Toast.LENGTH_SHORT).show();

                                    filePath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Uri> task) {
                                            RootRef.child("Users").child(currentUserID).child("image")
                                                    .setValue(task.getResult().toString())                  // In partea asta pun incarc imaginea
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {

                                                            if(task.isSuccessful()){

                                                                Toast.makeText(SettingsActivity.this, "Image saved in database succesfully", Toast.LENGTH_SHORT).show();
                                                                // Daca totul e ok primesti un mesaj ca a fost incarcata
                                                                loadingBar.dismiss();

                                                            }else {

                                                                String message = task.getException().toString();
                                                                Toast.makeText(SettingsActivity.this, "Error" + message, Toast.LENGTH_SHORT).show();
                                                                loadingBar.dismiss();
                                                            }
                                                        }
                                                    });
                                        }
                                    });


                        } else {

                            String message = task.getException().toString();
                            Toast.makeText(SettingsActivity.this, "Error" + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });
            }
        }
    }

    private void RetrieveUserInfo() {

        RootRef.child("Users").child(currentUserID)  // Creez legatura cu baza de date pentru user-ul respectiv
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name"))
                        && (dataSnapshot.child("image").getValue()!=null)){ // Daca acesta are poza si nume si bineinteles un status atunci luam toata informatia despre el


                            String retrieveUserName = dataSnapshot.child("name").getValue().toString(); // username
                            String retrieveStatus = dataSnapshot.child("status").getValue().toString(); // status
                            String retrieveImage = dataSnapshot.child("image").getValue().toString(); // imaginea
                            userName.setText(retrieveUserName);
                            userStatus.setText(retrieveStatus);
                            Picasso.get().load(retrieveImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                            System.out.println(retrieveUserName + " " + retrieveStatus + retrieveImage + " afisare");
                        }
                        else if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("name"))) { // Si aici daca nu are imagine de profil ia doar stausul si numele

                            String retrieveUserName = dataSnapshot.child("name").getValue().toString();
                            String retrieveStatus = dataSnapshot.child("status").getValue().toString();
                            userName.setText(retrieveUserName);
                            userStatus.setText(retrieveStatus);
                            System.out.println(retrieveUserName + " " + retrieveStatus + " afisare");
                        }
                        else {

                            userName.setVisibility(View.VISIBLE);
                            Toast.makeText(SettingsActivity.this,"Please set and update your profile information..",Toast.LENGTH_SHORT).show(); // daca nu are nimic completat atunc..
                                                                                                                                                            // atunci ii apare un mesaj
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void SendUserToMainActivity() {

        Intent mainIntent = new Intent(this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}

