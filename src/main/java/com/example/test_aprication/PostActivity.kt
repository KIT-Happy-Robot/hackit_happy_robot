package com.example.test_aprication

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_aprication.model.Post
import com.example.test_aprication.model.Emotion

class PostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_post)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.post)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //見るボタンを押したらFeed画面に遷移
        val buttonSee = findViewById<Button>(R.id.button_see)
        buttonSee.setOnClickListener {
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
        }

        //投稿ボタンを押したら入力されている文字列をPost.postにいれて、表示されている入力内容を消す
        val buttonPost = findViewById<Button>(R.id.button_post)
        val editPost = findViewById<EditText>(R.id.edit_post)
        buttonPost.setOnClickListener {
            val postText = editPost.text.toString()
            val newPost = Post(post = "",emotion = "", level = null, userId = "")
            newPost.post = postText
            val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            newPost.userId = prefs.getString("user_id", "") ?: ""
            editPost.setText("")
            Log.d("postData", newPost.toString())
        }

        //取得したEmotionデータから枠の色を変える
        val viewBox = findViewById<View>(R.id.view_box)
        val nowEmotion = Emotion(emotion = "", level = 0)
        //テスト用データ
        val emotionList = listOf("happy", "sad", "fun", "bad")
        nowEmotion.emotion = emotionList.random()
        nowEmotion.level = (0..20).random()

        //感情レベルの処理
        fun levelToAlpha(level: Int): Int {
            return ((level / 20.0) * 205 + 50).toInt()
        }
        val color = when (nowEmotion.emotion.lowercase()) {
            "happy" -> Color.argb(levelToAlpha(nowEmotion.level), 255, 235, 59)
            "sad"   -> Color.argb(levelToAlpha(nowEmotion.level), 33, 150, 243)
            "fun" -> Color.argb(levelToAlpha(nowEmotion.level), 76, 175, 80)
            else    -> Color.argb(levelToAlpha(nowEmotion.level), 244, 67, 54)
        }
        viewBox.setBackgroundColor(color)
        buttonPost.setBackgroundColor(color)
        buttonSee.setBackgroundColor(color)
    }
}