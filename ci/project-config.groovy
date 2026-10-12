// Project-specific, non-secret settings consumed by ci/Jenkinsfile.
// Keep passwords, API keys, and Webhook URLs in Jenkins Credentials.
return [
    git: [
        url          : 'https://github.com/weifangtao/testDemo.git',
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
                checkTask: 'compileStagingDebugKotlin',
                lintTask: 'lintStagingDebug',
                qualityTask: 'test :app:lintStagingDebug',
                packageTask: 'assembleStagingDebug',
                variant: 'stagingDebug',
                apkSearchDir: 'app/build/outputs/apk/staging/debug',
                requireSigning: false,
                aliases: ['test', 'debug']
            ],
            prod: [
                checkTask: 'compileProdReleaseKotlin',
                lintTask: 'lintProdRelease',
                qualityTask: 'test :app:lintProdRelease',
                packageTask: 'assembleProdRelease',
                variant: 'prodRelease',
                apkSearchDir: 'app/build/outputs/apk/prod/release',
                requireSigning: true,
                aliases: ['release']
            ]
        ],
        apkPattern    : '**/build/outputs/apk/**/*.apk'
    ],
    version: [
        baseName : '1.0.0',
        startCode: 100
    ],
    // Jenkins Secret text credential IDs; never put the AES values in this file.
    wmsAuth: [
        keyCredential: '',
        ivCredential : ''
    ],
    // Jenkins Credentials ID；正式 prod 发布必须全部填写
    signing: [
        keystoreFile : '',
        storePassword: '',
        keyAlias     : '',
        keyPassword  : ''
    ],
    pgyer: [:],
    feishu: [:]
]
