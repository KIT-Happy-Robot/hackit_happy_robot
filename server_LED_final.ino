// C++ code
//
#include <WiFiS3.h>
#include <ArduinoMqttClient.h>
#include <ArduinoJson.h>
 

// wifiのSSIDとパスワード
const char* SSID     = "";
const char* PASSWORD = "";

// サーバのURL
const char* BROKER = "broker.hivemq.com";
const int   PORT   = 1883;
 
const char* TOPIC_SUB = "emotion/broadcast";  // 受信用
const char* TOPIC_PUB = "kit/test";           // 送信用（←同じでも良い）
 
WiFiClient   wifi;
MqttClient   mqtt(wifi);



int counter;
int counter2;
int counter3;
int counter4;

void setup()
{
  Serial.begin(9600);
 
  // --- Wi-Fi ------------------------------------------------------------
  Serial.print("Connecting Wi-Fi");
  WiFi.begin(SSID, PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }
  Serial.println("\n✅ Wi-Fi connected");
 
  // --- MQTT ------------------------------------------------------------
  Serial.print("Connecting MQTT");
  while (!mqtt.connect(BROKER, PORT)) {
    Serial.print(".");
    delay(1000);
  }
  Serial.println("\n✅ MQTT connected");
 
  // 購読開始
  mqtt.subscribe(TOPIC_SUB);


  pinMode(11, OUTPUT);
  pinMode(10, OUTPUT);
  pinMode(9, OUTPUT);
  pinMode(6, OUTPUT);
  pinMode(5, OUTPUT);
  pinMode(3, OUTPUT);

}

void loop()
{
  String emotion = "a";
  int level = 10;
  int time_L = 0;

  // 受信処理
  int msgSize = mqtt.parseMessage();
  if (msgSize) {
    String response_body = "";
    while (mqtt.available()) {
      // mqtt.read()で読み込んだ文字(char)を文字列の末尾に追加する
      response_body += (char)mqtt.read();
    }
    // このループが終わると、response_bodyに受信した文字列全体が格納されている
    Serial.println(response_body);
  
    //JsonDocument doc;   
    // サーバーから取得したと仮定するJSON文字列
    String json_data = response_body;

    // JSONを解析するためのメモリ領域を準備
    // このJSONデータなら96バイトもあれば十分
    StaticJsonDocument<96> doc;
    
    // JSON文字列を解析する
    DeserializationError error = deserializeJson(doc, json_data);

    // 解析に失敗した場合の処理
    if (error) {
      Serial.print("JSONの解析に失敗しました: ");
      Serial.println(error.c_str());
      return;
    }

    // --- 辞書のようにキーを使って要素を取り出す ---

    // "emotion"というキーで文字列を取得
    String emotion = doc["emotion"]; // "happy" が取り出される

    // "level"というキーで数値を取得
    int level = doc["level"]; // 20 が取り出される

    // 結果を表示
    Serial.println(emotion);
    Serial.println(level);


    time_L = (3.1 - level / 10);
    if (emotion == "happy") {
  
      for (counter = 0; counter < 4; ++counter) {
        digitalWrite(LED_BUILTIN, HIGH);
        delay(2);
        digitalWrite(LED_BUILTIN, HIGH);
        delay(2);
        analogWrite(11, 225);
        analogWrite(10, 105);
        analogWrite(9, 180);
        analogWrite(6, 225);
        analogWrite(5, 105);
        analogWrite(3, 180);
        delay(1000 * time_L); // Wait for 1000 * time_L millisecond(s)
        analogWrite(11, 225);
        analogWrite(10, 69);
        analogWrite(9, 0);
        analogWrite(6, 225);
        analogWrite(5, 69);
        analogWrite(3, 0);
        delay(1000 * time_L); // Wait for 1000 * time_L millisecond(s)
      }
    }
    if (emotion == "bad") {
      for (counter2 = 0; counter2 < 3; ++counter2) {
        analogWrite(11, 128);
        analogWrite(10, 0);
        analogWrite(9, 0);
        analogWrite(6, 128);
        analogWrite(5, 0);
        analogWrite(3, 0);
        delay(1000 * time_L); // Wait for 1000 * time_L millisecond(s)
        analogWrite(11, 128);
        analogWrite(10, 128);
        analogWrite(9, 128);
        analogWrite(6, 128);
        analogWrite(5, 128);
        analogWrite(3, 128);
        delay(1000 * time_L); // Wait for 1000 * time_L millisecond(s)
      }
    }
    if (emotion == "sad") {
      for (counter3 = 0; counter3 < 2; ++counter3) {
        analogWrite(11, 0);
        analogWrite(10, 0);
        analogWrite(9, 225);
        analogWrite(6, 0);
        analogWrite(5, 0);
        analogWrite(3, 225);
        delay(1000 * time_L); // Wait for 1000 * time_L millisecond(s)
        analogWrite(11, 0);
        analogWrite(10, 191);
        analogWrite(9, 225);
        analogWrite(6, 0);
        analogWrite(5, 191);
        analogWrite(3, 225);
        delay(1000 * time_L); // Wait for 1000 * time_L millisecond(s)
      }
    }
    if (emotion == "fun") {
      for (counter4 = 0; counter4 < 4; ++counter4) {
        analogWrite(11, 225);
        analogWrite(10, 140);
        analogWrite(9, 0);
        analogWrite(6, 225);
        analogWrite(5, 140);
        analogWrite(3, 0);
        delay(1000 * time_L); // Wait for 1000 * time_L millisecond(s)
        analogWrite(11, 225);
        analogWrite(10, 225);
        analogWrite(9, 0);
        analogWrite(6, 225);
        analogWrite(5, 225);
        analogWrite(3, 0);
        delay(1000 * time_L); // Wait for 1000 * time_L millisecond(s)
      }
    }
    analogWrite(11, 0);
    analogWrite(10, 0);
    analogWrite(9, 0);
    analogWrite(6, 0);
    analogWrite(5, 0);
    analogWrite(3, 0);
    }
  }
