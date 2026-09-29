# Android 一键 CI/CD 模板

`Jenkinsfile` 负责通用流程，`project-config.groovy` 负责项目差异，密钥统一放在
Jenkins Credentials。接入新项目只需复制 `ci/`、填写配置、创建一个 Pipeline Job。

项目定位：`k3_wms_android` 是实际主项目，`testCICD` 是一键 CI/CD Demo。

Demo 地址：

- GitHub：https://github.com/weifangtao/testCICD
- Jenkins：http://wms-appbuild.bestfulfill-inc.com:8080/job/testCICD/

快速步骤：

1. 复制 `ci/` 到项目根目录；
2. 修改仓库地址、默认分支、Android SDK、Gradle module/profile；
3. 在 Jenkins 选择 **Pipeline script from SCM**，脚本路径填 `ci/Jenkinsfile`；
4. 自动触发做编译检查，手动构建才打包和发布。

详细配置、凭据、初始化命令和验收标准见 [CI-CD-使用说明.md](CI-CD-使用说明.md)。
