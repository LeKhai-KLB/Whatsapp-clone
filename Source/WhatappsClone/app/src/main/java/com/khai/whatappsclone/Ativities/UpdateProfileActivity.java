package com.khai.whatappsclone.Ativities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.khai.whatappsclone.Models.User;
import com.khai.whatappsclone.databinding.ActivitySetupProfileBinding;
import com.khai.whatappsclone.databinding.ActivityUpdateProfileBinding;

public class UpdateProfileActivity extends AppCompatActivity {

    ActivityUpdateProfileBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri selectedImage;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating profile...");
        dialog.setCancelable(false);

        getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        String currentUid = getIntent().getStringExtra("uid");
        database = FirebaseDatabase.getInstance("https://whatappsclone-335fb-default-rtdb.asia-southeast1.firebasedatabase.app");
        storage = FirebaseStorage.getInstance();

        String currentName = getIntent().getStringExtra("name");
        String currentImage = getIntent().getStringExtra("image");
        String phone = getIntent().getStringExtra("phone");

        binding.nameBox.setText(currentName);
        Glide.with(UpdateProfileActivity.this).load(currentImage)
                .into(binding.imageView);

        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 45);
            }
        });

        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.nameBox.getText().toString();

                if(name.isEmpty()){
                    binding.nameBox.setError("Please type a name");
                }

                dialog.show();
                if(selectedImage != null){
                    StorageReference storageReference = storage.getReference().child("Profiles").child(auth.getUid());
                    storageReference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if(task.isSuccessful()){
                                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String imageUrl = uri.toString();
                                        String name = binding.nameBox.getText().toString();
                                        User user = new User(currentUid, name, phone, imageUrl);
                                        database.getReference()
                                                .child("users")
                                                .child(currentUid)
                                                .setValue(user)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        dialog.dismiss();
                                                        Intent intent = new Intent(UpdateProfileActivity.this, MainActivity.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                });
                                    }
                                });
                            }
                        }
                    });
                }
                else{
                    String imgUrl = "No Image";
                    if(currentImage != "No Image"){
                        imgUrl = currentImage;
                    }
                    User user = new User(currentUid, currentName, phone, imgUrl);
                    database.getReference()
                            .child("users")
                            .child(currentUid)
                            .setValue(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    dialog.dismiss();
                                    Intent intent = new Intent(UpdateProfileActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data != null){
            if(data.getData() != null){
                binding.imageView.setImageURI(data.getData());
                selectedImage = data.getData();
            }
        }
    }
}