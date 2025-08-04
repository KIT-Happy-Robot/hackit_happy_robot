# 安定版のPythonとビルドツールが含まれたイメージに変更
FROM python:3.11

# 作業ディレクトリを設定
WORKDIR /app

# requirements.txtを先にコピーしてライブラリをインストール
# これにより、ソースコードの変更だけであればキャッシュが効き、ビルドが高速化する
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# アプリケーションのソースコードをコピー
COPY . .

# Cloud Runが提供するPORT環境変数をリッスンする
# デフォルトは8080
CMD ["gunicorn", "-k", "uvicorn.workers.UvicornWorker", "--bind", "0.0.0.0:${PORT}", "main:app"]