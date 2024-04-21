package top.linesoft.open2share;

import android.animation.ObjectAnimator;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SaveAsActivity extends AppCompatActivity {
    ExecutorService executorService;
    private static final String TAG = "SaveAsActivity";

    Handler mHandler = new Handler(Looper.getMainLooper());
    ActivityResultLauncher<Uri> openDocument = null;
    /**
     * 选择存储目录时，传入文件uri存到此处
     */
    List<Uri> inputFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_as);
        openDocument = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri o) {
                if (o == null) {
                    finishAffinity();
                } else {
                    save(inputFiles, o);
                }
            }
        });
        executorService = Executors.newCachedThreadPool();
        boolean saveAs = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("save_as", false);

        CircularProgressIndicator progressBar = findViewById(R.id.progress_circular);
        MaterialCardView cardView = findViewById(R.id.card);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cardView.setVisibility(View.VISIBLE);
                ObjectAnimator.ofFloat(cardView, "alpha", 0, 1).setDuration(200).start();
                progressBar.show();
            }
        }, 300);

        Intent intent = getIntent();
        List<Uri> uris = parseIntent(intent);

        if (uris.isEmpty()) {
            finishAffinity();
        } else {
            if (!saveAs) {
                inputFiles = uris;
                chooseFolder();
            } else {
                save(uris, null);
            }
        }
    }

    /**
     * Parse the intent to get the URI List of the incoming data
     */
    private List<Uri> parseIntent(Intent intent) {
        ArrayList<Uri> list = new ArrayList<>();
        if (intent.getAction() == null) {
            return list;
        }
        if (intent.getAction().equals(Intent.ACTION_SEND)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            list.add(uri);
        } else if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri uri = intent.getData();
            list.add(uri);
        } else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }
        return list;
    }

    private void chooseFolder() {
        //Optionally, add an initial path
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            openDocument.launch(Uri.fromFile(downloadDir));
        } else {
            openDocument.launch(null);
        }
    }


    private void save(List<Uri> uris, Uri targetFolder) {
        if (uris == null || uris.isEmpty()) {
            finishAffinity();
        } else if (uris.size() == 1) {
            executorService.submit(() -> {
                try {
                    saveFileToDownloadFolder(uris.get(0), targetFolder);
                } catch (Exception e) {
                    Log.e(TAG, "save: error", e);
                } finally {
                    mHandler.post(this::finishAffinity);
                }
            });
        } else {
            final CountDownLatch count = new CountDownLatch(uris.size());
            executorService.submit(() -> {
                try {
                    count.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    mHandler.post(this::finishAffinity);
                }
            });

            for (Uri uri : uris) {
                executorService.submit(() -> {
                    try {
                        saveFileToDownloadFolder(uri, targetFolder);
                    } catch (Exception e) {
                        Log.e(TAG, "run: error", e);
                    } finally {
                        count.countDown();
                    }
                });

            }

        }
    }


    @WorkerThread
    private void saveFileToDownloadFolder(Uri inputUri, Uri targetFolder) {

        try (InputStream in = getContentResolver().openInputStream(inputUri)) {
            DocumentFile documentFile = DocumentFile.fromSingleUri(this, inputUri);//input file
            Uri outUri = null;//input file output to here
            if (targetFolder != null) {
                //get target folder and create a new file under the target folder
                DocumentFile file = DocumentFile.fromTreeUri(this, targetFolder)
                        .createFile(documentFile.getType(), documentFile.getName());
                outUri = file.getUri();
            } else {
                //下载路径
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.Downloads.DISPLAY_NAME, documentFile.getName());
                    contentValues.put(MediaStore.Downloads.MIME_TYPE, documentFile.getType());
                    //Insert the record into the mediaStore to get a new file
                    outUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                } else {
                    //Directly obtain the download directory path and create a new file under the downloaded folder
                    File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    DocumentFile file = DocumentFile.fromFile(downloadDir)
                            .createFile(documentFile.getType(), documentFile.getName());
                    outUri = file.getUri();
                }
            }

            // output file
            if (outUri != null && in != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(outUri);
                if (outputStream != null) {

                    byte[] b = new byte[2048];
                    int len = -1;
                    while ((len = in.read(b)) != -1) {
                        outputStream.write(b, 0, len);
                    }
                    outputStream.flush();
                    outputStream.close();
                }
            } else {
                Log.d(TAG, "save: outUri = null");
            }
        } catch (Exception e) {
            Log.e(TAG, "saveFileToDownloadFolder: error", e);
            throw new RuntimeException(e);
        }
    }
}
