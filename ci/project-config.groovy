// Project-specific, non-secret settings consumed by ci/Jenkinsfile.
// Keep passwords, API keys, and Webhook URLs in Jenkins Credentials.
return [
    git: [
        url          : 'https://github.com/weifangtao/testCICD.git',
        // 仓库为 Public 时留空即可；私有仓库再填写 Jenkins Credentials ID。
        credentials  : '',
        defaultBranch: 'origin/main'
    ],
    jenkins: [
        // 当前 Jenkins 构建节点实际可用的 JDK 路径。
        // 节点安装 JDK 17 后，将此值替换为真实的 JDK 17 JAVA_HOME。
        javaHome      : '/usr/lib/jvm/java-21-openjdk-amd64',
        androidSdk    : '/var/lib/jenkins/android-sdk',
        gradleUserHome: '/var/lib/jenkins/.gradle'
    ],
    gradle: [
        module        : ':app',
        defaultProfile: 'test',
        profiles      : [
            test: [
                checkTask: 'compileDebugKotlin',
                packageTask: 'assembleDebug',
                variant: 'debug',
                aliases: ['debug']
            ],
            release: [
                checkTask: 'compileReleaseKotlin',
                packageTask: 'assembleRelease',
                variant: 'release',
                aliases: []
            ]
        ],
        apkPattern    : '**/build/outputs/apk/**/*.apk'
    ],
    pgyer: [:],
    feishu: [:]
]
