# Androidアプリのアーキテクチャにそってテストの書き方を学ぼう

## ハンズオンについて 

このハンズオンでは「[Now in Android App](https://github.com/android/nowinandroid)」のアプリを題材に、アプリアーキテクチャのレイヤにそってテストの書き方を学ぶ。<br>
<br>
<br>
<img src="./docs/handson/images/app_architecture.jpg" width="480">
<br>
<br>

Now in Android Appで使用されている技術は次のとおりで、これらの技術を使ったアプリのテスト方法を解説する。

- Kotlin Coroutine
- Kotlin Flow
- Room
- Data Store
- ViewModel(Android Architecture Component)
- Jetpack Compose
- Dagger Hilt

## ハンズオンの進め方

1. 本Repositoryをクローンし、カレントディレクトリをAndroid Studio (Electric Eel|2022.1.1+)で開く
2. Build Variantを`demoExerciseDebug`にする
3. 目次から学習したいセクションのページに遷移する
4. セクションのページにそって、練習問題を解く


## 目次

### [データレイヤをテストする](./docs/handson/DataLayerTest.md)

- API通信をするコードのテストを実装しながらCoroutineのテストについて学ぶ
- データソースに応じたテストの書き方を学ぶ
  - データベース(Room)のテストを書く
  - DataStoreのテストを書く
  - オンメモリキャッシュのテストを書く

### [UIレイヤをテストする](./docs/handson/UILayerTest.md)

- ViewModelをテストする
-  Jetpack Composeをテストする
    - Composeのユニットテストについて学ぶ
    - ViewModelを結合してComposeをテストする
    - ComposeのNavigationをテストする

## オリジナルのNow in Android Appからの変更点

 - README.mdを[README.original.md](./README.original.md)にリネームし、ハンズオン用のREADME.mdを追加
 - ハンズオンの内容を記載したmarkdown及びmarkdownで利用する画像をdocs/handson配下に追加
 - ハンズオンの演習と解答を1つのブランチで管理できるようにビルドバリアントを追加
 - ハンズオンで利用する実装クラスとテストコードを追加
 - すでにあった実装及びテストコードをハンズオンの演習内容にあわせて修正
 - 依存ライブラリのアップデート
 - テストで利用するライブラリを依存関係に追加
 - Github Actionsの設定ファイルを削除

# License

Original Copyright 2022 The Android Open Source Project. See [README.original.md](./README.original.md) for details.

Modifications Copyright 2023 DeNA Co., Ltd.

Licensed under the Apache License, Version 2.0.

