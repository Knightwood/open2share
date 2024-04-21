package top.linesoft.open2share;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;


public class ReceiveOpenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean b = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("use_file_uri", false);
        Uri uri = getIntent().getData();
        if (uri == null || uri.getScheme() == null) {
            finish();
        } else {
            if (uri.getScheme().equals("file")) {
                if (b) {
                    //API24以上系统分享支持file:///开头
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    builder.detectFileUriExposure();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.no_use_file_uri_msg, Toast.LENGTH_LONG).show();
                    finishAffinity();
                }
            }
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendIntent.addCategory("android.intent.category.DEFAULT");
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sendIntent.setType(getIntent().getType());
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ActivityResultLauncher<Intent> launcher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult o) -> finish());
            launcher.launch(Intent.createChooser(sendIntent, getString(R.string.share_title)));
        }


    }
}
