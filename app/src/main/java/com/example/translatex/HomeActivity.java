package com.example.translatex;


import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;


public class HomeActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, TranslateFragment.newInstance())
                    .commitNow();
        }
    }
}

