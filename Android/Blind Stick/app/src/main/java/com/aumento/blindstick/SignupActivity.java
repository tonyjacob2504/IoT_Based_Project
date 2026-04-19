package com.aumento.blindstick;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.aumento.blindstick.Utils.GlobalPreference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {


    private EditText nameET, emailET, phoneET, passwordET, emgET;
    private Button registerButton;
    private String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        GlobalPreference preference = new GlobalPreference(this);
        ip = preference.RetriveIP();

        initViews();

        registerButton.setOnClickListener(v -> {
            String name = nameET.getText().toString();
            String email = emailET.getText().toString();
            String phone = phoneET.getText().toString();
            String emgPhone = emgET.getText().toString();
            String password = passwordET.getText().toString();

            if( name.isEmpty() || email.isEmpty() || phone.isEmpty() || emgPhone.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }else if (!isValidPhoneNumber(phone) || !isValidPhoneNumber(emgPhone)){
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            }else if (!isValidEmail(email)){
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            }
            else
                register();
        });

    }

    private void register() {

        String name = nameET.getText().toString();
        String email = emailET.getText().toString();
        String phone = phoneET.getText().toString();
        String emgPhone = emgET.getText().toString();
        String password = passwordET.getText().toString();

        StringRequest request = new StringRequest(Request.Method.POST, "http://"+ip+"/blindstick/user_register.php", s -> {
            Log.d("******", "onResponse: "+s.trim());
            String result = s.trim();
            try {
                JSONObject jsonObject = new JSONObject(result);
                String status = jsonObject.getString("status");
                String message = jsonObject.getString("message");

                Toast.makeText(SignupActivity.this, message, Toast.LENGTH_SHORT).show();

                if(status.equals("success")){
                    finish();
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }, volleyError -> {
            Log.d("******", "onErrorResponse: "+volleyError);
            Toast.makeText(SignupActivity.this, "Please Try Again!", Toast.LENGTH_SHORT).show();
        }){
            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("email", email);
                params.put("phone", phone);
                params.put("emgPhone", emgPhone);
                params.put("password", password);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(SignupActivity.this);
        queue.add(request);

    }

    private void initViews() {

        nameET = findViewById(R.id.nameEditText);
        emailET = findViewById(R.id.emailEditText);
        phoneET = findViewById(R.id.phoneEditText);
        emgET = findViewById(R.id.emgEditText);
        passwordET = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
    }

    public boolean isValidPhoneNumber(String number) {
        String phoneNumberPattern = "^(\\+91[-\\s]?)?[6-9]\\d{9}$";
        return number.matches(phoneNumberPattern);
    }

    public boolean isValidEmail(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return email.matches(emailPattern);
    }
}