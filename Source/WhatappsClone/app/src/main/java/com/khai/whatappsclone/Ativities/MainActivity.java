package com.khai.whatappsclone.Ativities;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.security.identity.CipherSuiteNotSupportedException;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.khai.whatappsclone.Adapters.TopStatusAdapter;
import com.khai.whatappsclone.Models.Status;
import com.khai.whatappsclone.Models.User;
import com.khai.whatappsclone.Models.UserStatus;
import com.khai.whatappsclone.R;
import com.khai.whatappsclone.Adapters.UsersAdapter;
import com.khai.whatappsclone.databinding.ActivityMainBinding;
import com.khai.whatappsclone.databinding.DeleteDialogBinding;
import com.khai.whatappsclone.databinding.SettingsDialogBinding;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import jp.wasabeef.glide.transformations.BlurTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    FirebaseDatabase database;
    ArrayList<User> users;
    UsersAdapter usersAdapter;
    TopStatusAdapter topStatusAdapter;
    ArrayList<UserStatus> userStatuses;
    ProgressDialog progressDialog;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseRemoteConfig mFirebaseRemoteConfig  = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        mFirebaseRemoteConfig.fetchAndActivate().addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {

                String backgroundImage = mFirebaseRemoteConfig.getString("backgroundImage");
                Boolean backgroundImageEnable = mFirebaseRemoteConfig.getBoolean("backgroundImageEnable");

                if(backgroundImageEnable){
                    Glide.with(MainActivity.this).load(backgroundImage)
                            .into(binding.backgroundImage);
                    binding.backgroundImage.setAlpha(127);
                    binding.backgroundImage.setVisibility(View.VISIBLE);
                }
                else{
                    binding.backgroundImage.setVisibility(View.GONE);
                }

                // Toolbar color
                String toolbarColor = mFirebaseRemoteConfig.getString("toolbarColor");
                String toolbarImage = mFirebaseRemoteConfig.getString("toolbarBackground");
                Boolean toolbarImageEnable = mFirebaseRemoteConfig.getBoolean("toolbarImageEnable");

                if(toolbarImageEnable){
                    Glide.with(MainActivity.this)
                            .load(toolbarImage)
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull @NotNull Drawable resource, @Nullable @org.jetbrains.annotations.Nullable Transition<? super Drawable> transition) {
                                    getSupportActionBar().setBackgroundDrawable(resource);
                                }

                                @Override
                                public void onLoadCleared(@Nullable @org.jetbrains.annotations.Nullable Drawable placeholder) {

                                }
                            });
                }
                else{
                    getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(toolbarColor)));
                }

            }
        });

        database = FirebaseDatabase.getInstance("https://whatappsclone-335fb-default-rtdb.asia-southeast1.firebasedatabase.app");


        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String token) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("token",token);
                database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                        .updateChildren(map);
                //  Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading image...");
        progressDialog.setCancelable(false);

        users = new ArrayList<>();
        userStatuses = new ArrayList<>();

        usersAdapter = new UsersAdapter(this, users);
        topStatusAdapter = new TopStatusAdapter(this, userStatuses);
//        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.statusList.setLayoutManager(linearLayoutManager);

        binding.statusList.setAdapter(topStatusAdapter);
        binding.recyclerView.setAdapter(usersAdapter);

        binding.recyclerView.showShimmerAdapter();
        binding.statusList.showShimmerAdapter();

        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for(DataSnapshot snapshot1: snapshot.getChildren()){
                    User user = snapshot1.getValue(User.class);
                    if(!user.getUid().equals(FirebaseAuth.getInstance().getUid()) && user != null){
                        users.add(user);
                    }
                }
                binding.recyclerView.hideShimmerAdapter();
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        user = snapshot.getValue(User.class);
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });

        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    userStatuses.clear();
                    for(DataSnapshot snapshot1: snapshot.getChildren()){
                        UserStatus status = new UserStatus();
                        status.setName(snapshot1.child("name").getValue(String.class));
                        status.setProfileImage(snapshot1.child("profileImage").getValue(String.class));
                        status.setLastUpdated(snapshot1.child("lastUpdated").getValue(Long.class));

                        ArrayList<Status> statuses = new ArrayList<>();
                        for(DataSnapshot snapshot2: snapshot1.child("statuses").getChildren()){
                            Status status1 = snapshot2.getValue(Status.class);
                            statuses.add(status1);
                        }

                        status.setStatuses(statuses);
                        userStatuses.add(status);
                    }
                    binding.statusList.hideShimmerAdapter();
                    topStatusAdapter.notifyDataSetChanged();
                }
                else {
                    binding.statusList.hideShimmerAdapter();
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        binding.bottomNavigationView2.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.status:
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(intent, 75);
                        break;
                }
                return false;
            }
        });

    }



    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Online");
    }

//    @Override
//    protected void onPause() {
//        String currentId = FirebaseAuth.getInstance().getUid();
//        database.getReference().child("presence").child(currentId).setValue("Offline");
//        super.onPause();
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data != null){
            if(data.getData() != null){
                progressDialog.show();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                Date date = new Date();
                StorageReference reference = storage.getReference().child("status").child(date.getTime() + "");

                reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    UserStatus userStatus = new UserStatus();
                                    userStatus.setName(user.getName());
                                    userStatus.setProfileImage(user.getProfileImage());
                                    userStatus.setLastUpdated(date.getTime());

                                    HashMap<String, Object> obj = new HashMap<>();
                                    obj.put("name", userStatus.getName());
                                    obj.put("profileImage", userStatus.getProfileImage());
                                    obj.put("lastUpdated", userStatus.getLastUpdated());

                                    String imgUrl = uri.toString();

                                    Status status = new Status(imgUrl, userStatus.getLastUpdated());

                                    database.getReference().child("stories")
                                            .child(FirebaseAuth.getInstance().getUid())
                                            .updateChildren(obj);

                                    database.getReference().child("stories")
                                            .child(FirebaseAuth.getInstance().getUid())
                                            .child("statuses")
                                            .push()
                                            .setValue(status);

                                    progressDialog.dismiss();
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void searchUsers(String s){
        Query query = FirebaseDatabase.getInstance("https://whatappsclone-335fb-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users").orderByChild("name")
                .startAt(s.toUpperCase())
                .endAt(s.toLowerCase()+"\uf8ff");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for(DataSnapshot snap: snapshot.getChildren()){
                    User user = snap.getValue(User.class);
                    if(!user.getUid().equals(FirebaseAuth.getInstance().getUid())){
                        users.add(user);
                    }
                }
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.groups:
                startActivity(new Intent(MainActivity.this, GroupChatActivity.class));
                break;
            case R.id.search:
                binding.relative.setVisibility(View.VISIBLE);

                binding.icCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        binding.relative.setVisibility(View.GONE);
                    }
                });

                binding.messageBox.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        searchUsers(binding.messageBox.getText().toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                break;
            case R.id.settings:
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.settings_dialog, null);
                SettingsDialogBinding binding = SettingsDialogBinding.bind(view);
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Settings")
                        .setView(binding.getRoot())
                        .create();

                binding.updateProfile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, UpdateProfileActivity.class);
                        intent.putExtra("name", user.getName());
                        intent.putExtra("image", user.getProfileImage());
                        intent.putExtra("uid", user.getUid());
                        intent.putExtra("phone", user.getPhoneNumber());
                        MainActivity.this.startActivity(intent);
                        dialog.dismiss();
                    }
                });

                binding.cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                binding.logOut.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(MainActivity.this, PhoneNumberActivity.class));
                        finishAffinity();
                    }
                });

                dialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}