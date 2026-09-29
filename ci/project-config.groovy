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
        // 可选项：填写 Jenkins 节点上真实存在的 JAVA_HOME。
        // 留空时由 Jenkins PATH 提供 Java，流水线不会注入不存在的路径。
        javaHome      : '',
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
