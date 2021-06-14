# AndKit
## Getting started

在项目的根节点的 `build.gradle` 中添加如下代码
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

在项目的 `build.gradle` 中添加
```
dependencies {
    implementation 'com.github.wangpeiyuan:AndKit:v0.0.1'
}
```