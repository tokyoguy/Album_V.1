package album.com

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    var auth: FirebaseAuth? = null
    var googleSignInClient: GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        button_login_button.setOnClickListener {
            registerAndLoginUsers()
        }
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        button_login_google.setOnClickListener {
            googleLogin()
        }
    }

    fun registerAndLoginUsers() {

        if(email_edittext_login.text.toString().isEmpty() || password_edittext_login.text.toString().isEmpty()) {
            Toast.makeText(this, "Заполните поля email и пароль!", Toast.LENGTH_SHORT).show()
            return
        }
        auth?.createUserWithEmailAndPassword(email_edittext_login.text.toString(), password_edittext_login.text.toString())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val progressDialog = ProgressDialog(this)
                    progressDialog.setMessage("Создаём пользовательский аккаунт..")
                    progressDialog.setCancelable(true)
                    progressDialog.show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    loginUsers()
                }
            }
    }
    fun loginUsers() {
        auth?.signInWithEmailAndPassword(email_edittext_login.text.toString(), password_edittext_login.text.toString())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val progressDialog = ProgressDialog(this)
                    progressDialog.setMessage("Производим верификацию аккаунта..")
                    progressDialog.setCancelable(true)
                    progressDialog.show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }?.addOnFailureListener {
                Toast.makeText(this, "Введите корректный email и пароль!", Toast.LENGTH_SHORT).show()
            }
    }
    fun googleLogin() {
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_LOGIN_CODE) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result.isSuccess) {
                var account = result.signInAccount
                firebaseAuthWithGoogle(account)
            }
        }
    }
    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    loginUsers()
                }
            }
    }
}
