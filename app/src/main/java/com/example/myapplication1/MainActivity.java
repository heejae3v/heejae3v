package com.example.myapplication1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {



    AmazonS3 s3;
    TransferUtility util;
    TextView Separation;
    Button uploadButton;
    TextView progresss;
    ImageView selectedImage;
    Button resultButton;
    private final static int PICK_FROM_GALLERY = 833;
    private final static int GALLERY_REQUEST = 318;

    String textfile = null;
    String identityPool = "ap-northeast-2:b022f164-69c5-4e7a-a288-a95190e3fe7e";
    String bucket = "t4app";
    String bucket1 = "t4output";
    Regions region = Regions.AP_NORTHEAST_2;
    File downloadFromS3;
    Timer t = new Timer();
    String result; // 인공지능이 분류한 값 담는 문자열
    String label; //텍스트 뷰에 띄울 값
    String probability;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createAWSCredentials();

        uploadButton = findViewById(R.id.upload);
        progresss = findViewById(R.id.Value);
        selectedImage = findViewById(R.id.imageView);
        resultButton = findViewById(R.id.classfication);
        Separation = findViewById(R.id.Separate);

        uploadButton.setOnClickListener(view -> {
            //Upload Action TODO:-
            MainActivity.this.requestImageGallery();
        });
        resultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progresss.setText(String.valueOf(result));
                classfication();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    public void createAWSCredentials() {
        CognitoCachingCredentialsProvider cognitoService = new CognitoCachingCredentialsProvider(getApplicationContext() ,
               identityPool, region);



        s3 = new AmazonS3Client(cognitoService);

        s3.setRegion(Region.getRegion(region));;

        prepareTransferUtility();
    }

    private void prepareTransferUtility() {
        util = TransferUtility.builder().s3Client(s3).context(getApplicationContext()).build();
    }


    private void requestImageGallery() {
        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE};
            if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                ActivityCompat.requestPermissions((Activity) MainActivity.this, PERMISSIONS, GALLERY_REQUEST);
            } else {
                openGallery();
            }
        } else {
            openGallery();
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void openGallery() {
        Intent galleryIntent = new Intent();
        galleryIntent.setType("*/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.putExtra("return-data", true);
        startActivityForResult(galleryIntent, PICK_FROM_GALLERY);
    }
    public void gobject() { //인공지능이 분류한 결과값이 담긴 파일을 s3에서 가져옴
        try {
            S3Object s3Object = s3.getObject(new GetObjectRequest(bucket1,
                    textfile));
            InputStreamReader streamReader = new InputStreamReader(s3Object.getObjectContent(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader); //파일을 직접 받지않고 값만 읽어서 처리
            System.out.println(reader);
            result = reader.readLine();
            System.out.println(result);
            label = result.substring(0, result.indexOf(","));
            //probability = result.substring(result.indexOf("0"));
            System.out.println(label);
            //System.out.println(probability);
            reader.close();
            streamReader.close();
        }
        catch (Exception e){
            e.printStackTrace();

        }
    }
    public void classfication(){
        switch(label){
            case "plastic":
                Separation.setText("플라스틱입니다 : 내용물을 비우고 부착물을 제거한 후에 버려주세요");
                break;
            case "food":
                Separation.setText("음식물입니다 : 수분을 최대한 제거한 후 버려주세요");
                break;
            case "paper" :
                Separation.setText("종이입니다 : 비닐 코팅된 광고지, 비닐류, 기타오물이 섞이지 않도록 버려주세요");
                break;
            case "matal" :   //metal을 라벨링을 잘못찍었습니다.
                Separation.setText("금속입니다 : 내용물을 비우고 다른 재질은 제거후에 버려주세요");
                break;
            case "plastic bag" :
                Separation.setText("비닐입니다 : 내용물을 비우고 흩날리지 않도록 봉투에 담아 버려주세요");
                break;
            case "styrofoam" :
                Separation.setText("스티로폼입니다 : 내용물을 비우고 부착물을 제거한 후에 버려주세요");
                break;
            case "clothing" :
                Separation.setText("의류입니다 : 폐의류 전용 수거함에 버려주세요, 전용수거함이 없는경우 물에 젖지 않도록 마대에 담아서 버려주세요");
                break;
            case "glass" :
                Separation.setText("유리병류입니다 : 내용물을 비우고, 부착물을 제거한 후에  깨지지 않도록 주의하여 버려주세요");
                break;
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this, "Comes inside!", Toast.LENGTH_SHORT).show();


        if (requestCode == PICK_FROM_GALLERY) {

            if (resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();

                String[] proj = {MediaStore.MediaColumns.DATA};
                Cursor cursor = getContentResolver().query(selectedImageUri, proj, null, null, null);

                if (cursor == null) {
                    File file =  new File(selectedImageUri.getPath());

                    String filePath = selectedImageUri.getPath();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    selectedImage.setImageBitmap(bitmap);
                    //System.out.print(filePath);
                    uploadImageToAWS(file);
                    System.out.println(file);
                    TimerTask t_task = new TimerTask() {
                        @Override
                        public void run() {
                            gobject();
                        }
                    };
                    t.schedule(t_task,5000);
                    //progresss.setText(String.valueOf(label));
                } else {
                    File file =  new File(RealFilePath.getPath(this, selectedImageUri));
                    String filePath = RealFilePath.getPath(this, selectedImageUri);
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    //System.out.print(filePath);
                    selectedImage.setImageBitmap(bitmap);
                    uploadImageToAWS(file);
                    System.out.println(file);
                    TimerTask t_task = new TimerTask() {
                        @Override
                        public void run() {
                            gobject();
                        }
                    };
                    t.schedule(t_task,5000);
                    //progresss.setText(String.valueOf(label));
                }
            } else {
                Toast.makeText(this, "Picture is not selected!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void uploadImageToAWS(File imagedata) {
        try {
            String fileName = imagedata.getName();
            String cutname = fileName;
            TransferObserver observer = util.upload(bucket, fileName, imagedata, CannedAccessControlList.PublicRead);
            System.out.println(cutname);  //
            textfile = cutname.substring(0, cutname.indexOf("."))+ ".txt";
            System.out.println(textfile);
            observer.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    Log.d("State", state.toString());
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                    int percentage = (int) ((double) bytesCurrent * 100 / bytesTotal);
//                Toast.makeText(getApplicationContext(), percentage, Toast.LENGTH_SHORT).show();
//                progresss.setText(percentage);

                    Log.d("Percentage", String.valueOf(percentage));
                }

                @Override
                public void onError(int id, Exception ex) {
//                Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }
    /*
    public void downloadToAWS() {

        TransferObserver observer = util.download(bucket1, new File(textfile));

        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d( "State" , state.toString());
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) ((double) bytesCurrent * 100 / bytesTotal);
//                Toast.makeText(getApplicationContext(), percentage, Toast.LENGTH_SHORT).show();
//                progresss.setText(percentage);

                Log.d( "Percentage" , String.valueOf(percentage));
            }

            @Override
            public void onError(int id, Exception ex) {
//                Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

     */


    /*
    private Collection<String> loadFileFromS3() {
        try (final S3Object s3Object = s3.getObject(bucket1,
                textfile);
             final InputStreamReader streamReader = new InputStreamReader(s3Object.getObjectContent(), StandardCharsets.UTF_8);
             final BufferedReader reader = new BufferedReader(streamReader)) {
            return reader.lines().collect(Collectors.toSet());

        }
        catch (final IOException e) {
            return Collections.emptySet();
        }
    }

     */
}



