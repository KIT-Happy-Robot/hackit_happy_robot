package com.example.test_aprication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_aprication.model.Emotion
import com.example.test_aprication.model.Feed

class FeedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feed)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.feed)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //書くボタンを押したらPost画面に遷移
        val buttonWrite = findViewById<Button>(R.id.button_write)
        buttonWrite.setOnClickListener {
            val intent = Intent(this, PostActivity::class.java)
            startActivity(intent)
        }

        //投稿を表示する
        val feedPost = Feed(post = "", emotion = "", level = 0)
        //テスト用データ
        val testFeed = listOf(
            Feed("今日も一日楽しかった！", "happy", 15),
            Feed("アイス食べて元気出た！", "happy", 8),
            Feed("友だちとゲームして大笑い🤣", "fun", 12),
            Feed("授業むずかしすぎる〜", "bad", 9),
            Feed("雨で気分さがる☔", "sad", 6),
            Feed("宿題やり忘れた…", "bad", 14),
            Feed("体育でヘトヘト💦", "sad", 5),
            Feed("推しの配信で爆笑した！", "fun", 18),
            Feed("テストで満点とった！", "happy", 20),
            Feed("なんかモヤモヤする😶", "bad", 3),
            Feed("先生が優しくて嬉しかった😊", "happy", 10),
            Feed("思い出し笑いしちゃったw", "fun", 7),
            Feed("大事なメッセージ見逃した😢", "sad", 17),
            Feed("今日はなんか全部うまくいった！", "happy", 19),
            Feed("楽しくおしゃべりできた", "fun", 13)
        )

        //テスト用
        var counter = 0
        fun test(): Int {
            counter += 1
            return counter
        }

        val viewPost = findViewById<TextView>(R.id.view_post)
        val viewBox = findViewById<View>(R.id.view_box)

        //感情レベルの処理
        fun levelToAlpha(level: Int): Int {
            return ((level / 20.0) * 205 + 50).toInt()
        }
        //post表示関数。
        fun displayPost(view: View, textView: TextView, post: String, emotion: String, level: Int) {
            textView.text = post
            val color = when (emotion.lowercase()) {
                "happy" -> Color.argb(levelToAlpha(level), 255, 235, 59)     // 黄色💛
                "sad"   -> Color.argb(levelToAlpha(level), 33, 150, 243)     // 青💙
                "fun"   -> Color.argb(levelToAlpha(level), 76, 175, 80)      // 緑💚
                else    -> Color.argb(levelToAlpha(level), 244, 67, 54)      // 赤❤️
            }
            view.setBackgroundColor(color)
            buttonWrite.setBackgroundColor(color)
        }

        val firstPost = testFeed[0]
        displayPost(viewBox, viewPost, firstPost.post, firstPost.emotion, firstPost.level)

        //リアクションボタンを押したらemotionデータを作る
        val buttonHappy = findViewById<Button>(R.id.button_happy)
        val buttonSad = findViewById<Button>(R.id.button_sad)
        val buttonFun = findViewById<Button>(R.id.button_fun)
        val buttonBad = findViewById<Button>(R.id.button_bad)
        buttonHappy.setOnClickListener {
            val reaction = Emotion(emotion = "", level = 0)
            reaction.emotion = "happy"
            reaction.level = 10
            Log.d("emotion", reaction.toString())
            displayPost(viewBox, viewPost, testFeed[test()].post, testFeed[test()].emotion, testFeed[test()].level)
        }
        buttonSad.setOnClickListener {
            val reaction = Emotion(emotion = "", level = 0)
            reaction.emotion = "sad"
            reaction.level = 10
            Log.d("emotion", reaction.toString())
            val num = test()
            displayPost(viewBox, viewPost, testFeed[num].post, testFeed[num].emotion, testFeed[num].level)
        }
        buttonFun.setOnClickListener {
            val reaction = Emotion(emotion = "", level = 0)
            reaction.emotion = "fun"
            reaction.level = 10
            Log.d("emotion", reaction.toString())
            val num = test()
            displayPost(viewBox, viewPost, testFeed[num].post, testFeed[num].emotion, testFeed[num].level)
        }
        buttonBad.setOnClickListener {
            val reaction = Emotion(emotion = "", level = 0)
            reaction.emotion = "Bad"
            reaction.level = 10
            Log.d("emotion", reaction.toString())
            val num = test()
            displayPost(viewBox, viewPost, testFeed[num].post, testFeed[num].emotion, testFeed[num].level)
        }

    }
}