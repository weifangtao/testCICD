# keystore 目录

把 `release.jks` 放在这里（**已被 .gitignore 忽略，不会入库**）。

生成方式：

```bash
keytool -genkeypair -v \
  -keystore keystore/release.jks \
  -alias testdemo \
  -keyalg RSA -keysize 2048 -validity 10000
```

然后 `cp keystore.properties.example keystore.properties` 并填入密码。
