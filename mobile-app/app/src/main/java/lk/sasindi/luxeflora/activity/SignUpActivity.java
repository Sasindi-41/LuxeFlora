package lk.sasindi.luxeflora.activity;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import lk.sasindi.luxeflora.model.User;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;

    private FirebaseAuth firebaseAuth;

    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignUpBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        TextView signin = findViewById(R.id.signup_btn_signin);
        signin.setPaintFlags(signin.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        binding.signupBtnSignin.setOnClickListener(view -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });
        binding.signupBtnSignup.setOnClickListener(view -> {

            String name = binding.signupInputName.getText().toString().trim();
            String email = binding.signupInputEmail.getText().toString().trim();
            String password = binding.signupInputPassword.getText().toString().trim();
            String retypePassword = binding.signupInputRetypePassword.getText().toString().trim();

            if (name.isEmpty()) {
                binding.signupInputName.setError("Name is required");
                binding.signupInputName.requestFocus();
                return;
            }
            if (email.isEmpty()) {
                binding.signupInputEmail.setError("Email is required");
                binding.signupInputEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.signupInputEmail.setError("Enter a valid email");
                binding.signupInputEmail.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                binding.signupInputPassword.setError("Password is required");
                binding.signupInputPassword.requestFocus();
                return;
            }
            if (password.length() < 8) {
                binding.signupInputPassword.setError("Password must contain at least 8 characters");
                binding.signupInputPassword.requestFocus();
                return;
            }
            if (retypePassword.isEmpty()) {
                binding.signupInputRetypePassword.setError("Retype Password is required");
                binding.signupInputRetypePassword.requestFocus();
                return;
            }
            if (!retypePassword.equals(password)) {
                binding.signupInputRetypePassword.setError("Password and Retype Password mest be the same");
                binding.signupInputRetypePassword.requestFocus();
                return;
            }

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            String uid = task.getResult().getUser().getUid();

                            User user = User.builder()
                                    .uid(uid)
                                    .name(name)
                                    .email(email)
                                    .build();

                            firebaseFirestore.collection("users")
                                    .document(uid)
                                    .set(user)
                                    .addOnSuccessListener(unused -> {

                                        Toast.makeText(SignUpActivity.this,
                                                "User registered successfully",
                                                Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                                        startActivity(intent);
                                        finish();

                                    })
                                    .addOnFailureListener(e -> {

                                        Toast.makeText(SignUpActivity.this,
                                                "Firestore Error: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();

                                    });

                        } else {

                            Toast.makeText(SignUpActivity.this,
                                    "Auth Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();

                        }

                    });

        });
    }
}