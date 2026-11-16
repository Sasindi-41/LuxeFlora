package lk.sasindi.luxeflora.activity;

import android.graphics.Paint;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import lk.sasindi.luxeflora.R;
import lk.sasindi.luxeflora.databinding.ActivityForgotPasswordBinding;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        TextView forgotBtnBack = findViewById(R.id.forgot_btn_back);
        forgotBtnBack.setPaintFlags(forgotBtnBack.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        binding.forgotBtnSend.setOnClickListener(v -> {
            String email = binding.forgotInputEmail.getText().toString().trim();

            if(email.isEmpty()){
                binding.forgotInputEmail.setError("Email is required");
                binding.forgotInputEmail.requestFocus();
                return;
            }
            if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                binding.forgotInputEmail.setError("Enter a valid email");
                binding.forgotInputEmail.requestFocus();
                return;
            }

            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(ForgotPasswordActivity.this,
                                        "Password reset email sent. Check your inbox.",
                                        Toast.LENGTH_LONG).show();
                                finish(); // go back to SignInActivity
                            } else {
                                Toast.makeText(ForgotPasswordActivity.this,
                                        "Failed to send reset email. Try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        binding.forgotBtnBack.setOnClickListener(v -> finish());
    }
}