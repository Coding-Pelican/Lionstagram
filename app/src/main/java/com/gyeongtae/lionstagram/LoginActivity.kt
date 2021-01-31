package com.gyeongtae.lionstagram

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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.gyeongtae.lionstagram.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var activityLoginBinding: ActivityLoginBinding
    var auth: FirebaseAuth? = null
    var googleSignInClient: GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001    //  구글 로그인 시 사용할 리퀘스트 값
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(activityLoginBinding.root)

        activityLoginBinding.btnEmailLogin.setOnClickListener {
            signinAndSignup()
        }
        activityLoginBinding.btnGoogleLogin.setOnClickListener {
            //google login first step
            googleLogin()
        }
        //구글 로그인 옵션 만들기
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  //  구글 API 키
            .requestEmail() //  email 아이디 요청
            .build()    //  build로 닫기

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //firebase 로그인 통합 관리하는 객체 만들기
        auth = FirebaseAuth.getInstance()
    }

    fun googleLogin() {
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_LOGIN_CODE) {
            //구글에서 넘겨주는 로그인 결과값 받아오기
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result != null) {
                if (result.isSuccess) { //로그인 성공 시
                    //이 값을 파이어베이스에 넘길수 있도록 만들어 주기
                    var account = result.signInAccount
                    //Second Step
                    firebaseAuthWithGoogle(account!!)
                } else{
                }
            }
        }
    }

    //구글 로그인 성공시 토큰값을 파이어베이스로 넘겨주어서 계정을 생성하는 코드
    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        //account 안에 있는 token id를 넘겨주기
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //로그인 성공(id, pw 일치)
                    moveMainPage(task.result?.user)
                } else {
                    //로그인 실패시 에러
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun signinAndSignup() {
        auth?.createUserWithEmailAndPassword(
            activityLoginBinding.etEmail.text.toString(),
            activityLoginBinding.etPassword.text.toString()
        )
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //아이디 생성 성공 시
                    moveMainPage(task.result?.user)
                } else if (task.exception?.message.isNullOrEmpty()) {
                    //공백 일 경우
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                } else {
                    //이미 계정이 있는 경우
                    signinEmail()
                }
            }
    }

    fun signinEmail() {
        auth?.signInWithEmailAndPassword(
            activityLoginBinding.etEmail.text.toString(),
            activityLoginBinding.etPassword.text.toString()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                //로그인 성공(id, pw 일치)
                moveMainPage(task.result?.user)
            } else {
                //로그인 실패시 에러
                Toast.makeText(this, task.exception!!.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    //로그인 성공 시 다음 페이지로 넘어가는 함수
    fun moveMainPage(user: FirebaseUser?) { //  firebaseUser상태를 넘겨줌
        if (user != null) { //  user가 있을 경우
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}