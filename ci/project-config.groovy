// Project-specific, non-secret settings consumed by ci/Jenkinsfile.
// Keep passwords, API keys, and Webhook URLs in Jenkins Credentials.
return [
    git: [
        url          : 'https://github.com/weifangtao/testCICD.git',
        credentials  : 'github-testcicd',
        defaultBranch: 'origin/main'
    ],
    jenkins: [
        javaHome      : '/var/lib/jenkins/jdk17',
        androidSdk    : '/var/lib/jenkins/android-sdk',
        gradleUserHome: '/var/lib/jenkins/.gradle'
    ],
    gradle: [
        module        : ':app',
        defaultProfile: 'test',
        profiles      : [
            test: [
                task   : 'assembleDebug',
                variant: 'debug',
                aliases: ['debug']
            ],
            release: [
                task   : 'assembleRelease',
                variant: 'release',
                aliases: []
            ]
        ],
        apkPattern    : '**/build/outputs/apk/**/*.apk'
    ],
    pgyer: [:],
    feishu: [:]
]
