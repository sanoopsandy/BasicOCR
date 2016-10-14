package com.example.sanoop.tesseract;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.theartofdev.edmodo.cropper.CropImageView;

public class SimpleAndroidOCRActivity extends Activity {
	public static final String PACKAGE_NAME = "com.example.sanoop.tesseract";
	public final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Android/data/" + PACKAGE_NAME +  "/";
//	public static String DATA_PATH = "/storage/0/storage/emulated/0/Define/";

	// You should have the trained data file in assets folder
	// You can get them at:
	// http://code.google.com/p/tesseract-ocr/downloads/list
	public static final String lang = "eng";

	private static final String TAG = "MyApplication.java";

    protected ImageView imgBit;
	protected EditText ocrText;
	protected CropImageView cropImageView;
	protected boolean _taken;
	static final int REQUEST_IMAGE_CAPTURE = 1;
	static final int MY_PERMISSION_CAMERA = 0;
    private Uri picUri;

    protected static final String PHOTO_TAKEN = "photo_taken";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ocrText = (EditText) findViewById(R.id.field);
        imgBit = (ImageView) findViewById(R.id.imgBit);
        cropImageView = (CropImageView) findViewById(R.id.cropImageView);
        checkPermissions();
	}

    private void checkPermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA},
                    MY_PERMISSION_CAMERA);
            return;
        }

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSION_CAMERA);
            return;
        }
    }

    public void openCam(View v){
		Log.v(TAG, "Starting Camera app");
		startCameraActivity();
	}

	public void startCameraActivity() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.i(TAG, "resultCode: " + resultCode);

		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            picUri = data.getData();
            onPhotoTaken(data);
        }
        else {
			Log.v(TAG, "User cancelled");
		}
	}

    public static Bitmap createContrast(Bitmap src, double value) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
        int A, R, G, B;
        int pixel;
        // get contrast value
        double contrast = Math.pow((100 + value) / 100, 2);

        // scan through all pixels
        for(int x = 0; x < width; ++x) {
            for(int y = 0; y < height; ++y) {
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                // apply filter contrast for every channel R, G, B
                R = Color.red(pixel);
                R = (int)(((((R / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if(R < 0) { R = 0; }
                else if(R > 255) { R = 255; }

                G = Color.red(pixel);
                G = (int)(((((G / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if(G < 0) { G = 0; }
                else if(G > 255) { G = 255; }

                B = Color.red(pixel);
                B = (int) (((((B / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if(B < 0) { B = 0;
                } else if(B > 255) { B = 255; }
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        return bmOut;
    }

    private void applyOCR(Bitmap bitmap) {
        bitmap = createContrast(bitmap, 100);
        Log.v(TAG, "Before baseApi");
        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setDebug(true);
        baseApi.init(DATA_PATH, lang);
        baseApi.setImage(bitmap);
        String recognizedText = baseApi.getUTF8Text();
        baseApi.end();
        imgBit.setImageBitmap(bitmap);

        Log.v(TAG, "OCRED TEXT: " + recognizedText);

        if ( lang.equalsIgnoreCase("eng") ) {
            recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9]+", " ");
        }

        recognizedText = recognizedText.trim();

        if ( recognizedText.length() != 0 ) {
            ocrText.setText(ocrText.getText().toString().length() == 0 ? recognizedText : ocrText.getText() + " " + recognizedText);
            ocrText.setSelection(ocrText.getText().toString().length());
        }
    }

    @Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(SimpleAndroidOCRActivity.PHOTO_TAKEN, _taken);
	}

    @Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.i(TAG, "onRestoreInstanceState()");
//		if (savedInstanceState.getBoolean(SimpleAndroidOCRActivity.PHOTO_TAKEN)) {
//			onPhotoTaken();
//		}
	}

	public void makeTessFolder(){
		File mFilePath = new File(DATA_PATH);
		if(!mFilePath.exists()) {
			mFilePath.mkdirs();
		}
		if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
			try {

				AssetManager assetManager = getAssets();
				InputStream in = assetManager.open("tessdata/eng.traineddata");
				File yourFile = new File(DATA_PATH + "tessdata/eng.traineddata");
				yourFile.getParentFile().mkdirs(); // Will create parent directories if not exists
				yourFile.createNewFile(); // if file already exists will do nothing
                OutputStream out = new FileOutputStream(yourFile, false);
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
				Log.v(TAG, "Copied " + lang + " traineddata");
			} catch (IOException e) {
				Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
			}
		}
	}

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public void onPhotoTaken(Intent data) {
		ocrText.setText("");
		_taken = true;
		makeTessFolder();
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 4;
		Bundle extras = data.getExtras();
		Bitmap bitmap = (Bitmap) extras.get("data");
        assert bitmap != null;
        picUri = getImageUri(getApplicationContext(), bitmap);
		ExifInterface exif = null;
		int rotate = 0;
		try {
			exif = new ExifInterface(picUri.getPath());
			int exifOrientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			Log.v(TAG, "Orient: " + exifOrientation);
			switch (exifOrientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					rotate = 90;
					break;
                case ExifInterface.ORIENTATION_ROTATE_180:
					rotate = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					rotate = 270;
					break;
			}
		}catch (IOException e) {
			e.printStackTrace();
		}

		Log.v(TAG, "Rotation: " + rotate);

		if (rotate != 0) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            Matrix mtx = new Matrix();
            mtx.preRotate(rotate);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
        }
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        applyOCR(bitmap);
	}
}
