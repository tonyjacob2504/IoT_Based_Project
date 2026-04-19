package com.aumento.caregiver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.aumento.caregiver.Utils.GlobalPreference;

public class IPActivity extends AppCompatActivity {

    private GlobalPreference mGlobalPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ipactivity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mGlobalPreference= new GlobalPreference(getApplicationContext());
        mGlobalPreference.addUID("1");
        getIP();
    }

    public void getIP(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("    Enter Your IP Address    ");

        final EditText input = new EditText(IPActivity.this);
        input.setText(mGlobalPreference.RetriveIP());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getApplicationContext(), "Text entered is " + input.getText().toString(), Toast.LENGTH_SHORT).show();

                mGlobalPreference.addIP(input.getText().toString());

                input.setText(input.getText().toString());
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();

            }
        });
        builder.show();

    }

}