# オンメモリキャッシュのテストを書く

このセクションでは、オンメモリキャッシュをデータソースにもつRepositoryのテストの書き方を学ぶ。

## テスト方針

通信結果をオンメモリに保存して、キャッシュとして利用する実装のテストについて考える。<br>
(ここでのオンメモリキャッシュはRoomのオンメモリDBのことではなく、キャッシュを保持するクラスのインスタンスの破棄とともに消えるキャッシュのことを指す。)

オンメモリキャッシュは、データベースやファイルなど外部へのアクセスがないため、オンメモリ用のI/Fが定義されていなくてもテスト実装ができる。単純なケースだと、データソースを作らずにRepositoryが直接キャッシュをもつこともある。<br>
そのため、Repositoryを通じたテストのみで十分なことが多く、テストのセットアップのハードルはほぼない。<br>
一方で、キャッシュの有効かどうかの判定に時間を使うときは、テストコードから時間をコントロールできるようにする必要がある。

ここでは、次の2つのテストの書き方を紹介する。

 - キャッシュがある場合はキャッシュが利用され、不必要なAPIリクエストが行われていないか 
 - キャッシュが有効でなくなったときに、APIリクエストが行われてデータが更新されるか

## ハンズオンの対象コード

### テスト対象クラス`OnMemoryCacheVideoNewsRepository`の実装

[OnMemoryCacheVideoNewsRepository](../../core/data/src/exercise/java/com/google/samples/apps/nowinandroid/core/data/repository/OnMemoryCacheVideoNewsRepository.kt)

テストコードは[OnMemoryCacheVideoNewsRepositoryTest](../../core/data/src/testExercise/java/com/google/samples/apps/nowinandroid/core/data/repository/OnMemoryCacheVideoNewsRepositoryTest.kt)

テストしたいコードの実装を抜粋する。getVideoNewsResourcesはキャッシュがあり、かつキャッシュが有効期限内であればオンメモリのキャッシュを返す。そうでなければ、APIリクエストを行い、通信結果をキャッシュに保存する。<br>
キャッシュの有効期限は10分である。

```kotlin
private var videoNewsCache = emptyList<VideoNewsResource>()
private var cacheCreatedAt: Long = 0L

override suspend fun getVideoNewsResources(): List<VideoNewsResource> {

  if (videoNewsCache.isNotEmpty() && isCacheExpired().not()) {
    return this.videoNewsCache
  }
  
  val videoNewsResource = networkDataSource
    .getVideoNewsResources()
    .map(NetworkVideoNewsResource::asModel)

  this.videoNewsCache = videoNewsResource
  this.cacheCreatedAt = System.currentTimeMillis()
  return videoNewsResource
}

private fun isCacheExpired(): Boolean {
  return System.currentTimeMillis() - cacheCreatedAt > CACHE_EXPIRED_MILLS
}
```

API通信部分は任意の処理を差し込めるように、[TestNetworkDataSource](../../core/data/src/test/java/com/google/samples/apps/nowinandroid/core/data/testdoubles/TestNetworkDataSource.kt)を利用する。

### 練習問題で修正するファイルと解答例

<!-- textlint-disable japanese/sentence-length -->
- `demoExerciseDebug`ビルドバリアントで [`core/data/src/testExercise/java/com/google/samples/apps/nowinandroid/core/data/repository/OnMemoryCacheVideoNewsRepositoryTest.kt`](../../core/data/src/testExercise/java/com/google/samples/apps/nowinandroid/core/data/repository/OnMemoryCacheVideoNewsRepositoryTest.kt) を開いて作業する
- `demoAnswerDebug` ビルドバリアントに切り替えると解答例を確認できる
<!-- textlint-disable japanese/sentence-length -->


## 不必要なAPIリクエストが行われていないかをテストする

キャッシュが利用されているかを検証するためには、テストしたいメソッドを2回実行した上で次の2つを検証する。

- APIリクエストの回数が1回であること
- 2回目の戻り値が、TestNetworkDataSourceで返すように設定したものと一致すること

テストコードは次のとおり。

```kotlin
@Test
fun `getVideoNewsResources`() = runTest {

    // APIリクエスト回数
    var callCount = 0

    val testNetworkDataSource = TestNetworkDataSource(getVideoNewsResourcesFunc = {
      // APIリクエスト回数を記録する
      callCount++
        listOf(
            testNetworkVideoNewsResource,
        )
    })

    val videoNewsRepository = OnMemoryCacheVideoNewsRepository(testNetworkDataSource)

    // 2回実行する
    videoNewsRepository.getVideoNewsResources() // 1回目なのでAPIリクエストが行われる
    val actual = videoNewsRepository.getVideoNewsResources() // キャッシュが返却される

    val expected = listOf(
        testVideoNewsResource,
    )
    Truth.assertThat(expected).isEqualTo(actual) // キャッシュの中身を検証
    Truth.assertThat(callCount).isEqualTo(1) // APIリクエスト回数
}

```


### 練習問題

`// TODO`部分を埋めてテストを完成させよう。
- テストメソッド： `getVideoNewsResources_不必要なAPIリクエストが行われていないかをテストする`
- テスト概要： `getVideoNewsResources`を2回呼び出しても、APIリクエストは1回しか行われず2回目はキャッシュが使われることを確認する


## キャッシュの有効期限切れのテストを実装する

キャッシュの有効期限切れのチェックをするために、`System.currentTimeMillis()`や`SystemClock.elapsedRealtime()`を使って現在時刻を取得するケースは
が多い。ロジックの判定に現在時刻を使う場合、現在時刻を返す処理を外から差し込めるようにしないとテストが書きづらくなる。

オンメモリキャッシュはキャッシュから10分間で有効期限切れとなり、`getVideoNewsResources`を呼び出した時点で有効期限が切れていればAPIリクエストをおこなう。
その振る舞いを、実際に10分間待たずにテスト実行できるように、現在時刻を外から渡せるようにする。

OnMemoryCacheVideoNewsRepositoryのコンストラクタに、現在時刻のミリ秒を返す実装を外から渡せるようにする(今回はFunction Typeを外から渡せるように修正)<br>
直接`System.currentTimeMillis()`を呼び出していたコードは、このFunction Type経由で時刻を取得するようにする。

```kotlin
class OnMemoryCacheVideoNewsRepository(
    private val networkDataSource: NiaNetworkDataSource,
    private val currentTimeMillsProvider: () -> Long = { System.currentTimeMillis() }
)
```

テストコードでは、次のように時刻を変更できるようになる。

```kotlin
@Test
fun `getVideoNewsResources`() = runTest {
    
    ..

    // ミリ秒をテストコードから差し込む
    var currentTimeMills = 0L
    val videoNewsRepository = OnMemoryCacheVideoNewsRepository(testNetworkDataSource,
        currentTimeMillsProvider = {
            currentTimeMills
        })

    // 1回目の実行
    videoNewsRepository.getVideoNewsResources()

    // 時間をすすめる
    currentTimeMills = MINUTES.toMillis(10) + 1  // 10分 + 1ミリ秒

    // 2回目の実行
    val actual = videoNewsRepository.getVideoNewsResources()

    Truth.assertThat(callCount).isEqualTo(2)
}
```

### 練習問題

`// TODO`部分を埋めてテストを完成させよう。
- テストメソッド： `getVideoNewsResources_キャッシュの有効期限切れのテストを実装する`
- テスト概要： キャッシュの有効期限が切れた後の`getVideoNewsResources`の実行時には、APIリクエストが行われることを確認する
- [OnMemoryCacheVideoNewsRepository](../../core/data/src/exercise/java/com/google/samples/apps/nowinandroid/core/data/repository/OnMemoryCacheVideoNewsRepository.kt)#getVideoNewsResourcesの中で、現在時刻を外から差し込めるようにする

## まとめ

- オンメモリキャッシュをデータソースにもつRepositoryでは、API呼び出し回数を記録することで不必要なAPIリクエストが行われていないかを確認できる
- キャッシュの有効期限切れに時刻を使う場合は、現在時刻を外から差し込めるように実装する
