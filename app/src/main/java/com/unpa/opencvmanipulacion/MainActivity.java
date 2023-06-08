package com.unpa.opencvmanipulacion;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(OpenCVLoader.initDebug()){
            Toast.makeText(getBaseContext(), "Se cargo OpenCv", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getBaseContext(), "No cargo OpenCv", Toast.LENGTH_SHORT).show();
        }
    }
}