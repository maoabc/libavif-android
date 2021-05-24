package com.nmmedit.libavif;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            final ImageView avifImage = findViewById(R.id.avif_img);
            final byte[] bytes = inputStreamToBytes(getAssets().open("test.avif"));
            Glide.with(this)
                    .load(bytes)
                    .into(avifImage);

            final ImageView avifsImage = findViewById(R.id.avifs_img);
            final byte[] bytes2 = inputStreamToBytes(getAssets().open("test.avifs"));
            Glide.with(this)
                    .load(bytes2)
                    .into(avifsImage);
        } catch (IOException e) {

        }
    }

    private byte[] inputStreamToBytes(InputStream is) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(64 * 1024);
        try {
            int nRead;
            byte[] data = new byte[16 * 1024];
            while ((nRead = is.read(data)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.close();
        } catch (IOException e) {
            return null;
        }
        return buffer.toByteArray();
    }
}