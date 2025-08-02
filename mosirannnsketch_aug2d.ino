// C++ code
// シャープ測距モジュール GP2Y0A21YK0Fを使用
// ピン11に赤色LED、ピン10に青色LEDを接続
// 
const int sensorPin = A0;  // センサーを接続するアナログピン
const int redLedPin = 11;  // 赤色LEDを接続するデジタルピン
const int blueLedPin = 10; // 青色LEDを接続するデジタルピン

// 判定用の閾値（センサーの特性や使い方に合わせて調整が必要）
const int threshold_fast = 110;  // ポンポンと判断する変化量の閾値
const int threshold_slow = 100;  // なでなでと判断する変化量の閾値
const int threshold_deadzone = 25; // ★★★ この値以下の変化は無視する ★★★

// 時間判定用の変数
long lastChangeTime = 0;
const long detectionDuration = 500; // 500ミリ秒以上なだらかな変化が続いたら「なでなで」と判定

// センサー値格納用
int currentValue = 0;
int previousValue = 0;

void setup() {
  Serial.begin(9600); // シリアル通信を開始
  pinMode(redLedPin, OUTPUT);
  pinMode(blueLedPin, OUTPUT);
  
  // 初期値を設定
  previousValue = analogRead(sensorPin);
  lastChangeTime = millis(); // ★★★ 起動直後の誤判定を防ぐために現在時刻で初期化 ★★★
}

void loop() {
  currentValue = analogRead(sensorPin);
  int valueChange = abs(currentValue - previousValue);

  // ポンポンの判定
  // センサー値が急激に変化した場合
  if (valueChange > threshold_fast) {
    digitalWrite(redLedPin, LOW);
    digitalWrite(blueLedPin, HIGH); // 青色LEDを点灯
    Serial.println("PonPon");       // シリアルモニターに「PonPon」と表示
    
    // ポンポンを検出したら、次のなでなで判定をリセット
    lastChangeTime = millis();
  } 
  
  // なでなでの判定
  // ★★★ 変化量がデッドゾーンより大きく、かつ閾値以下の場合 ★★★
  else if (valueChange > threshold_deadzone && valueChange <= threshold_slow && (millis() - lastChangeTime) > detectionDuration) {
    digitalWrite(redLedPin, HIGH);  // 赤色LEDを点灯
    digitalWrite(blueLedPin, LOW);
    Serial.println("NadeNade");     // シリアルモニターに「NadeNade」と表示
  }
  
  // どちらでもない場合はLEDを消灯し、何も表示しない
  else {
    digitalWrite(redLedPin, LOW);
    digitalWrite(blueLedPin, LOW);
  }

  // 次のループのために現在の値を保存
  previousValue = currentValue;
  
  delay(10);
}
