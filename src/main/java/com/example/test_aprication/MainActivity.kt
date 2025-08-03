package com.example.test_aprication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button
import android.content.Context
import androidx.core.content.edit
import java.util.UUID
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //userIDの取得、作成
        userId = getOrCreateUserId()
        Log.d("UserId", userId)


        val buttonStart = findViewById<Button>(R.id.button_start)

        //スタートボタンを押したらPost画面に遷移
        buttonStart.setOnClickListener {
            val intent = Intent(this, PostActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getOrCreateUserId(): String {
        val userPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val storedUserId = userPrefs.getString("user_id", null)

        return if (storedUserId != null) {
            storedUserId
        } else {
            val newId = UUID.randomUUID().toString()
            userPrefs.edit {
                putString("user_id", newId)
            }
            newId
        }
    }
}