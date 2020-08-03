package com.example.zeetasupport;

import android.animation.ArgbEvaluator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.anstrontechnologies.corehelper.AnstronCoreHelper;
import com.example.zeetasupport.adapters.ViewPagerAdapter;
import com.example.zeetasupport.data.Upload;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.iceteck.silicompressorr.FileUtils;
import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

public class FashionDesignerDashboard extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    ViewPager viewPager;
    //List<Model> modelList;
    //List<ModelForImage> modelList2;
    Integer[] colors = null;
    ArgbEvaluator evaluator = new ArgbEvaluator();
    AnstronCoreHelper coreHelper;
    ViewPagerAdapter adapter2;
    private StorageReference mDatabaseRef;
    //private List<Upload> mUploads;
    private RecyclerView mRecyclerView;
    private String id;
    private TextView uploadPic, deletePic;
    private ImageView photo;
    private ProgressBar uploadProgressBar;
    private Uri mImageUri;
    private StorageReference mStorageRef;
    private ValueEventListener mDBListner;
    private StorageTask mUploadTask;
    private DatabaseReference mUploadDatabaseRef;
    private ProgressDialog uploadProgressDialog;
    private List<Upload> mUpload;
    private ArrayList<String> imageUrlList;
    private int pageIndex;
    private long numberOfSamplePictures = 0;
    private String piclink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fashion_designer_dashboard);
        id = FirebaseAuth.getInstance().getUid();
        mUpload = new ArrayList<>();

        imageUrlList = new ArrayList();
        adapter2 = new ViewPagerAdapter(FashionDesignerDashboard.this, imageUrlList);

        uploadProgressDialog = new ProgressDialog(this);
        uploadProgressDialog.setMessage("Uploading...");

        mStorageRef = FirebaseStorage.getInstance().getReference("fashionPics").child(id);
        mUploadDatabaseRef = FirebaseDatabase.getInstance("https://zeeta-6b4c0.firebaseio.com").getReference("fashionPics").child(id);

        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("serviceproviderpictures");
        storageReference.getDownloadUrl();
        coreHelper = new AnstronCoreHelper(this);

        mDBListner = mUploadDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUpload.clear();
                imageUrlList.clear();
                int i = 0;
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    numberOfSamplePictures = dataSnapshot.getChildrenCount();

                    Log.d("count", "The number of pictures:" + dataSnapshot.getChildrenCount());

                    Upload upload = postSnapshot.getValue(Upload.class);
                    assert upload != null;
                    upload.setmKey(postSnapshot.getKey());
                    mUpload.add(upload);
                    imageUrlList.add(upload.getmImageUrl());
                    i++;
                }

                viewPager = findViewById(R.id.ViewPager);
                viewPager.setAdapter(adapter2);
                viewPager.setPadding(50, 5, 50, 0);

                adapter2.notifyDataSetChanged();

                uploadPic = findViewById(R.id.upload_photo);
                deletePic = findViewById(R.id.delete_photo);

                viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                        if (position < (adapter2.getCount() - 1) && position < (colors.length - 1)) {
                            viewPager.setBackgroundColor((Integer) evaluator
                                    .evaluate(positionOffset, colors[position], colors[position + 1]));
                        } else {
                            viewPager.setBackgroundColor(colors[colors.length - 1]);
                        }
                    }

                    @Override
                    public void onPageSelected(int position) {
                        pageIndex = position;
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }
                });
                colors = new Integer[]{getResources().getColor(R.color.blue3),
                        getResources().getColor(R.color.blue5),
                        getResources().getColor(R.color.colorAccent),
                        getResources().getColor(R.color.darkGrey)
                };

                uploadPic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (numberOfSamplePictures < 5) {
                            if (mUploadTask != null && mUploadTask.isInProgress()) {
                                Toast.makeText(FashionDesignerDashboard.this, "upload in progress", Toast.LENGTH_SHORT).show();
                            } else {
                                openFileChooser();
                            }
                        } else {
                            AlertDialog alertDialog;
                            alertDialog = new AlertDialog.Builder(FashionDesignerDashboard.this).create();
                            alertDialog.setTitle("Sample Pictures");
                            alertDialog.setMessage("Number of sample pictures exceeded, delete old samples in order to add new samples");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                        }

                    }
                });

                deletePic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        viewPager.getChildAt(pageIndex);
                        Upload uploadItem = mUpload.get(pageIndex);
                        String key = uploadItem.getmKey();
                        FirebaseStorage str = FirebaseStorage.getInstance();
                        StorageReference mstr = str.getReferenceFromUrl(uploadItem.getmImageUrl());
                        mstr.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                mUploadDatabaseRef.child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Toast.makeText(FashionDesignerDashboard.this, "Image deleted successfully!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(FashionDesignerDashboard.this, "Image deletion Unsuccessful!", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                });


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uploadProgressDialog.show();
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            mImageUri = data.getData();
            if (mImageUri != null) {
                uploadFile();
            }
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void uploadFile() {

        if (mImageUri != null) {
            long timeInMillis = System.currentTimeMillis();
            StorageReference fileReference = mStorageRef.child(timeInMillis + "." + getFileExtension(mImageUri));
            StorageReference filepath = fileReference;
            File file;
            file = new File(SiliCompressor.with(this)
                    .compress(FileUtils.getPath(this, mImageUri), new File(this.getCacheDir(), "temp")));
            Uri uri = Uri.fromFile(file);

            mUploadTask = fileReference.putFile(uri).
                    addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            uploadProgressDialog.dismiss();
                            //If file exist in storage this works.
                            filepath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    piclink = task.getResult().toString();
                                    // downloadurl will be the resulted answer
                                    String temp = piclink + "/" + timeInMillis + ".jpg";
                                    Toast.makeText(FashionDesignerDashboard.this, "Picture uploaded successfully!", Toast.LENGTH_SHORT).show();
                                    Upload upload = new Upload("Finally", piclink, "Temp String bla bla bla");
                                    String uploadId = mUploadDatabaseRef.push().getKey();
                                    assert uploadId != null;
                                    mUploadDatabaseRef.child(uploadId).setValue(upload);
                                }
                            });

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(FashionDesignerDashboard.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            uploadProgressDialog.dismiss();
            Toast.makeText(FashionDesignerDashboard.this, "No file selected!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUploadDatabaseRef.removeEventListener(mDBListner);
    }
}