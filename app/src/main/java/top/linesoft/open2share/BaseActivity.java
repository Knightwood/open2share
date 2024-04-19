package top.linesoft.open2share;

import android.app.UiModeManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class BaseActivity extends AppCompatActivity {

    FrameLayout getRoot(){
        return findViewById(android.R.id.content);
    }
    void edge2edge() {
        //edge2edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        View root = findViewById(android.R.id.content);

        //statusBar color
        boolean isLightTheme = true;
        UiModeManager uiModeManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        isLightTheme = uiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_NO;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//api23 以上
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), root);
            controller.setAppearanceLightStatusBars(isLightTheme);
            controller.setAppearanceLightNavigationBars(isLightTheme);
        } else {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            getWindow().getDecorView().setSystemUiVisibility(flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        //padding
        WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(getWindow().getDecorView());
        Insets insets = Insets.NONE;
        if (rootWindowInsets != null) {
            insets = rootWindowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
        }
        if (ViewCompat.isAttachedToWindow(root)) {
            root.setPadding(insets.left, insets.top, insets.right, insets.bottom);
        } else {
            Insets finalInsets = insets;
            root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {
                    root.removeOnAttachStateChangeListener(this);
                    root.setPadding(finalInsets.left, finalInsets.top, finalInsets.right, finalInsets.bottom);
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                }
            });
        }
    }

}
