# Android 一键 CI/CD 模板

`Jenkinsfile` 负责通用流程，`project-config.groovy` 负责项目差异，密钥统一放在
Jenkins Credentials。接入新项目只需复制 `ci/`、填写配置、创建一个 Pipeline Job。

项目定位：`k3_wms_android` 是实际主项目，`testDemo` 是一键 CI/CD Demo。

Demo 地址：

- GitHub：https://github.com/weifangtao/testDemo
- Jenkins：http://wms-appbuild.bestfulfill-inc.com:8080/job/testDemo/

快速步骤：

1. 复制 `ci/` 到项目根目录（或跑 `ci/init-android-project.sh` 一键生成）；
2. 修改仓库地址、默认分支、Android SDK、Gradle module/profile；
3. 注意 `gitParameter` 的**参数名**：插件没有 `credentialId` 字段，写了会编译失败
   （`bash ci/verify-template.sh` 会校验）；
4. 在 Jenkins 选择 **Pipeline script from SCM**，脚本路径填 `ci/Jenkinsfile`；
   也可以用 `ci/jenkins-setup.groovy` 在 Script Console 里一键创建/修复 Job；
5. 自动触发做编译检查，手动构建才打包和发布。

> ⚠️ 改完 `parameters` 后要**跑一次构建**（或在 Job 配置页点保存），Jenkins 才会重新
> 注册参数定义。否则「Build with Parameters」里看到的还是旧的参数类型。

详细配置、凭据、初始化命令和验收标准见 [CI-CD-使用说明.md](CI-CD-使用说明.md)，
分支下拉框排查见该文档第 10 节。
