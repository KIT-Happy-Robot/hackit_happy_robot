import os
import wave
import pyaudio
import whisper
import datetime
import requests
import json
from pydub import AudioSegment
import numpy 


def fetch_emotion_data(url):
    """
    指定されたURLから感情データを取得し、Pythonの辞書として返す関数。
    """
    print(f"データを取得しています: {url}")
    
    try:
        # URLに対してGETリクエストを送信
        response = requests.get(url, timeout=10) # 10秒でタイムアウト
        
        # ステータスコードが200番台（成功）以外の場合はエラーを発生させる
        response.raise_for_status()
        
        # レスポンスのJSONデータをPythonの辞書に変換
        data = response.json()
        
        return data

    except requests.exceptions.RequestException as e:
        # ネットワークエラー（接続不可、タイムアウトなど）
        print(f"エラー: ネットワーク接続に問題があります。 {e}")
        return None
    except json.JSONDecodeError:
        # JSONとして解析できない場合のエラー
        print("エラー: サーバーからの応答が正しいJSON形式ではありません。")
        return None
    
# --- 設定項目 ---
# Whisperのモデルを選択 (tiny, base, small, medium, large)
MODEL_NAME = "small"

# 録音設定
FORMAT = pyaudio.paInt16  # 16ビット整数フォーマット
CHANNELS = 1             # モノラル
RATE = 16000             # サンプリングレート (Whisperは16000Hzを想定)
CHUNK = 1024             # 一度に読み込むデータサイズ
INPUT_DEVICE_INDEX=2
WAITSECONDS=2

def record_and_transcribe():
    """
    マイクから音声を録音し、Whisperで文字起こしを行い、
    音声とテキストをファイルに保存するメイン関数。
    """
    print(f"Whisperモデル '{MODEL_NAME}' をロード中です... これには数秒かかります。")
    try:
        model = whisper.load_model(MODEL_NAME)
    except Exception as e:
        print(f"モデルのロード中にエラーが発生しました: {e}")
        print("インターネット接続を確認するか、モデルが正しくインストールされているか確認してください。")
        return

    p = pyaudio.PyAudio()

    stream = p.open(format=FORMAT,
                    channels=CHANNELS,
                    rate=RATE,
                    input=True,
                    frames_per_buffer=CHUNK,
                    input_device_index=INPUT_DEVICE_INDEX)

    print("\n--------------------------------------------------")
    print("録音を開始しました。話してください。")
    print("--------------------------------------------------")

    frames = []
    cnt = 0
    try:
        while True:
            data = stream.read(CHUNK)
            frames.append(data)
            x = numpy.frombuffer(data, dtype="int16") / 32768.0
            # 音量が閾値以下であれば、無音とみなす
            if x.max() < 0.05:
                cnt += 1
            else:
                cnt = 0
            # 無音状態が一定時間続いた場合に、無音状態と判定
            if cnt > WAITSECONDS / (CHUNK / RATE):
                cnt=0
                break
    except KeyboardInterrupt:
        print("\n録音を停止しました。")
    finally:
        # ストリームを安全に停止・終了
        stream.stop_stream()
        stream.close()
        p.terminate()

    print("音声データを処理中です...")

    # --- ファイル名の生成 ---
    now = datetime.datetime.now()
    timestamp = now.strftime("%Y%m%d_%H%M%S")
    # 一時的なWAVファイル名と、最終的な出力ファイル名
    temp_wav_filename = f"temp_{timestamp}.wav"
    output_mp3_filename = f"recording_{timestamp}.mp3"
    output_txt_filename = f"transcription_{timestamp}.txt"

    # --- 1. 一時的にWAVファイルとして保存 ---
    # PyAudioで録音した生データを、まずは標準的なWAV形式で保存します。
    with wave.open(temp_wav_filename, 'wb') as wf:
        wf.setnchannels(CHANNELS)
        wf.setsampwidth(p.get_sample_size(FORMAT))
        wf.setframerate(RATE)
        wf.writeframes(b''.join(frames))
    
    print(f"一時WAVファイル '{temp_wav_filename}' を保存しました。")

    # --- 2. WAVをMP3に変換 ---
    # pydubを使ってWAVファイルを読み込み、MP3形式でエクスポートします。
    try:
        sound = AudioSegment.from_wav(temp_wav_filename)
        sound.export(output_mp3_filename, format="mp3")
        print(f"MP3ファイル '{output_mp3_filename}' を保存しました。")
    except Exception as e:
        print(f"MP3への変換中にエラーが発生しました: {e}")
        print("FFmpegが正しくインストールされ、PATHが通っているか確認してください。")
        # エラーが発生しても文字起こしは続行できるように、WAVファイルを対象にする
        audio_source_for_whisper = temp_wav_filename
    else:
        audio_source_for_whisper = output_mp3_filename

    # --- 3. Whisperで文字起こし ---
    print("Whisperによる文字起こしを開始します... (音声の長さに応じて時間がかかります)")
    try:
        # language="ja" を指定すると、より日本語の認識精度が上がることがあります
        result = model.transcribe(audio_source_for_whisper, language="ja", fp16=False)
        transcribed_text = result["text"]
        print("\n--- 文字起こし結果 ---")
        print(transcribed_text)
        print("----------------------")

        # --- 4. テキストファイルに保存 ---
        with open(output_txt_filename, "w", encoding="utf-8") as f:
            f.write(transcribed_text)
        print(f"テキストファイル '{output_txt_filename}' を保存しました。")

    except Exception as e:
        print(f"文字起こし中にエラーが発生しました: {e}")
    finally:
        # --- 5. 一時WAVファイルを削除 ---
        if os.path.exists(temp_wav_filename):
            os.remove(temp_wav_filename)
            print(f"一時ファイル '{temp_wav_filename}' を削除しました。")

# --- 設定 ---
# 起動したVOICEVOXのIPアドレスとポート
HOST = "127.0.0.1"
PORT = 50021
# VOICEVOXのデフォルトサンプリングレートは24000Hzです
# これ以外の値を指定すると音の高さが変わってしまいます
SAMPLING_RATE = 40000

# 発音させる単語の辞書
FINNISH_WORDS = {
    "happy": "iloinen",
    "sad": "surullinen",
    "fun": "hauska",
    "bad": "huono"
}

def generate_and_play(text, speaker_id=63, speed=1.0, volume=1.0):
    """
    指定されたテキスト、話者、設定で音声を生成し再生する関数
    
    Args:
        text (str): 音声化するテキスト
        speaker_id (int): VOICEVOXの話者ID
        speed (float): 話速 (1.0が基準)
        volume (float): 音量 (1.0が基準)
    """
    try:
        # 1. 音声合成用のクエリを作成
        params = (
            ('text', text),
            ('speaker', speaker_id),
        )
        query_res = requests.post(
            f'http://{HOST}:{PORT}/audio_query',
            params=params
        )
        query_res.raise_for_status()  # エラーがあれば例外を発生させる
        query_data = query_res.json()

        # 2. クエリのパラメータを調整して音量と勢いを変更
        print(f"話速を{speed}に、音量を{volume}に設定します。")
        query_data["speedScale"] = speed
        query_data["volumeScale"] = volume
        # 他にもピッチ(pitchScale)や抑揚(intonationScale)なども変更可能です

        # 3. 調整したクエリで音声合成を実行
        synthesis_res = requests.post(
            f'http://{HOST}:{PORT}/synthesis',
            headers={"Content-Type": "application/json"},
            params={'speaker': speaker_id},
            data=json.dumps(query_data)
        )
        synthesis_res.raise_for_status()

        voice = synthesis_res.content

        # 4. pyaudioで再生処理
        p = pyaudio.PyAudio()
        stream = p.open(
            format=pyaudio.paInt16,
            channels=1,
            rate=SAMPLING_RATE,  # VOICEVOXのサンプリングレートに合わせる
            output=True
        )
        stream.write(voice)
        stream.stop_stream()
        stream.close()
        p.terminate()

        print(f"「{text}」を再生しました。")

    except requests.exceptions.RequestException as e:
        print(f"エラー: VOICEVOXエンジンに接続できませんでした。")
        print("VOICEVOXが起動しているか、ホストとポートの設定が正しいか確認してください。")
    except Exception as e:
        print(f"予期せぬエラーが発生しました: {e}")

def coll_volume(level):
    if 0 < level <= 5:
        volume = 0.5
    elif 5 < level <= 10:
        volume = 1.0
    elif 10 < level <= 15:
        volume = 1.5
    elif 15 < level <= 20:
        volume = 2.0
    else:
        volume = 0
    return volume

if __name__ == "__main__":
    # データを取得したいURL
    target_url = "https://hackitserver-563679032017.asia-east1.run.app"
    
    # 関数を呼び出してデータを取得
    emotion_data = fetch_emotion_data(target_url)

    emotion = emotion_data.get("emotion")
    level = emotion_data.get("level")

    record_and_transcribe()

    # 話者ID 63 中国うさぎ
    # 話者ID 3 ずんだもん
    DEFAULT_SPEAKER = 3

    count = 0


    while count < 3:
        
        text_to_say = FINNISH_WORDS[emotion]

        if 0 < level <= 5:
            rate = 0.5
        elif 5 < level <= 10:
            rate = 0.9
        elif 10 < level <= 15:
            rate = 1.3
        elif 15 < level <= 20:
            rate = 1.5
            
        # --- 再生実行 ---
        generate_and_play(
            text_to_say, 
            speaker_id=DEFAULT_SPEAKER, 
            speed=rate, 
            volume=coll_volume(level)
        )
        count += 1
    