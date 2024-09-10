# UIレイヤをテストする

## UIレイヤの役割

- UIレイヤはアプリデータを画面に表示する役割で、ユーザーの操作やAPIレスポンスなどの外部入力によるデータの変更をUIに反映する
- UIレイヤはState holdersとUI elementsで構成される
  - State holdersはUIに表示する状態(UI State)の保持と更新を管理する
  - UI elementsはユーザーイベントのState holderへの通知と、State holderがもつStateの内容のレンダリングを行う

<br>
<br>
<img src="./images/app_architecture.jpg" width="480">
<br>
<br>

## UIレイヤ State holders

State holdersはUIに表示する状態(UI State)の保持と更新を管理する役割。<br>
UI Stateは、画面の表示に必要な情報をまとめたもの。たとえばNewsUiStateにはニュース画面を表示するために必要な、ニュースの記事のリスト自体やその他の情報(ローディングやエラーを表示するかの情報等)をもつ。

State holdersのテストでは、主に次のこと確認する。

- ユーザーの操作やAPIレスポンスなどの外部入力を受けたときに、UI Stateがどのように変化するか
- ViewModelからデータレイヤへの呼び出しが適切にできているか

Androidアプリの場合、Configuration Changeでの画面再生成に対応するため、State holderとしてAndroid Architecture ComponentのViewModelを採用するケースが多い。そのため、このハンズオンではViewModelのテストについて紹介する。

ViewModelのテストはLocal Test(src/test配下)で実行する。AndroidフレームワークとViewModelを切り離した設計にできていればRobolectricも不要なため、高速にテストを実行できる。


## UIレイヤ UI Elements

UI elementsはユーザーイベントのState holderへの通知と、State holderがもつStateの内容のレンダリングを行う。

Androidアプリでは以下がUI elementsに対応する。

- Android View
- Jetpack ComposeにおけるComposable関数

UI elementsのテストでは、主に次のことを確認する。

- レイアウトに関するテスト
  - あるUI Stateが与えられたときに、意図とおりUIコンポーネントが配置されていることを確認する
- 操作に関するテスト
  - あるUIコンポーネントを操作したときに、意図とおりイベントが発火することを確認する
  - あるUIコンポーネントを操作したときに、それによってUI Stateが更新され、それによってUIが変化する(または画面遷移する)ことを確認する

UI elementsのテストは、いわゆるUIテストにあたる。
UIテストには、コード変更前後の画面スクリーンショットを比較し、意図しない差分がないことを確認するVisual Regression Test (VRT)も含まれる。
これらのテストはInstrumentation TestもしくはRobolectricで実行する。

このハンズオンではUI elementsのうち、Jetpack Composeのテストについて紹介する。Android Viewに対するテストについては「Androidテスト全書」などを参考にすること。



## 目次

- [ViewModelをテストする](./ViewModel.md)
-  Jetpack Composeをテストする
    - [Composeのユニットテストについて学ぶ](./UIElementTest_Compose.md)
    - [ViewModelを結合してComposeをテストする](./UIElementTest_ComposeWithViewModel.md)
    - [ComposeのNavigationをテストする](./UIElementTest_Navigation.md)
- Jetpack Composeの画面スクリーンショットを使ってVisual Regression Testを実現する
    - [Composeのプレビュー画面でVisual Regression Testを行う](./VisualRegressionTest_Preview.md)
    - [Visual Regression TestをCIで実行する](./VisualRegressionTest_CI.md)
    - [様々なケースでComposeの画面スクリーンショットを撮る](./VisualRegressionTest_Advanced.md)
    - [Composable Preview Scannerを使ってプレビュー画面のスクリーンショットを撮る](./VisualRegressionTest_Preview_ComposablePreviewScanner.md)
