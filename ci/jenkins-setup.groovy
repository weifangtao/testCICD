/* ══════════════════════════════════════════════════════════════════════
 * Jenkins Job 一键创建 / 修复脚本
 *
 * 用法：
 *   1. 打开 http://wms-appbuild.bestfulfill-inc.com:8080/script
 *   2. 按下面的「配置」改成本项目的信息
 *   3. 先保持 apply = false 执行一次 —— 只诊断，不改任何东西
 *   4. 看完诊断结论，把 apply 改成 true 再执行一次 → 创建/修复 Job
 *
 * 它解决三件事：
 *   - 检查 Git Parameter 插件是否安装（没装的话 BRANCH 必然是文本输入框）
 *   - 检查 Job 里 BRANCH 参数当前是下拉还是文本（Jenkins 会缓存旧定义）
 *   - 创建/修复 Job 的 SCM 与流水线脚本路径
 *
 * ⚠️ 这个脚本要在 Script Console（管理员、无沙箱）里跑。
 * ══════════════════════════════════════════════════════════════════════ */

import hudson.model.ParametersDefinitionProperty
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import hudson.triggers.SCMTrigger
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

// ─────────────────────────────────────────────────────────────────────
// 配置：按项目修改这一段
// ─────────────────────────────────────────────────────────────────────
def CFG = [
        jobName      : 'testDemo',
        repoUrl      : 'https://github.com/weifangtao/testDemo.git',
        scriptPath   : 'ci/Jenkinsfile',
        defaultBranch: '*/main',
        credentialsId: '',                 // 私有仓库填 Jenkins 凭据 ID
        pollSchedule : 'H/5 * * * *',
        apply        : false,              // 诊断模式；确认后改 true 再跑一次
]

// ═════════════════════════════════════════════════════════════════════
def jenkins = Jenkins.get()
def problems = []

def section = { title -> println "\n──────── ${title} ────────" }
def ok = { msg -> println "  ✅ ${msg}" }
def bad = { msg -> println "  ❌ ${msg}"; problems << msg }
def warn = { msg -> println "  ⚠️  ${msg}"; problems << msg }
def info = { msg -> println "  •  ${msg}" }

println "Jenkins: ${jenkins.getRootUrl()}  (${Jenkins.VERSION})"

// ── 1. 插件检查 ──────────────────────────────────────────────────────
section '插件'

def gitParamPlugin = jenkins.pluginManager.getPlugin('git-parameter')
if (gitParamPlugin == null) {
    bad '未安装 Git Parameter 插件 —— BRANCH 必然是纯文本输入框'
    println '     修复：系统管理 → 插件管理 → Available plugins → 搜索 "Git Parameter" → 安装并重启'
} else {
    ok "Git Parameter 插件已安装：v${gitParamPlugin.version}"
    if (gitParamPlugin.hasUpdate) {
        info "有新版本可用：${gitParamPlugin.getUpdateInfo()?.version}"
    }
}

def gitPlugin = jenkins.pluginManager.getPlugin('git')
gitPlugin ? ok("Git 插件：v${gitPlugin.version}") : bad('未安装 Git 插件')

def cpsPlugin = jenkins.pluginManager.getPlugin('workflow-cps')
cpsPlugin ? ok("Pipeline (workflow-cps)：v${cpsPlugin.version}") : bad('未安装 Pipeline 插件')

// ── 2. Job 与 BRANCH 参数检查 ────────────────────────────────────────
section "Job: ${CFG.jobName}"

def job = jenkins.getItemByFullName(CFG.jobName, WorkflowJob)

if (job == null) {
    warn "Job 不存在：${CFG.jobName}（apply=true 时会创建）"
} else {
    ok "Job 存在"

    // SCM 检查
    def defn = job.definition
    if (defn instanceof CpsScmFlowDefinition) {
        def configuredPath = defn.scriptPath
        configuredPath == CFG.scriptPath
                ? ok("流水线脚本路径：${configuredPath}")
                : warn("流水线脚本路径为 '${configuredPath}'，期望 '${CFG.scriptPath}'")

        def remote = defn.scm?.userRemoteConfigs?.find { it.url }
        def jobUrl = remote?.url
        if (jobUrl == null) {
            warn 'Job 的 SCM 里没有仓库地址 —— BRANCH 下拉框会列不出分支'
        } else if (jobUrl != CFG.repoUrl) {
            warn "Job SCM 仓库为 '${jobUrl}'，与期望 '${CFG.repoUrl}' 不一致"
        } else {
            ok "Job SCM 仓库：${jobUrl}"
        }
        remote?.credentialsId
                ? info("Job SCM 凭据：${remote.credentialsId}")
                : info('Job SCM 未配置凭据（公开仓库可忽略）')
    } else {
        warn "Job 的 definition 不是 Pipeline script from SCM（当前：${defn?.class?.simpleName}）"
    }

    // 参数检查 —— 这是「分支要手输」最常见的直接原因
    def prop = job.getProperty(ParametersDefinitionProperty)
    if (prop == null) {
        info 'Job 当前没有参数定义（首次构建后才会从 Jenkinsfile 注册）'
    } else {
        println '  BRANCH 及其他参数（Jenkins 缓存的当前定义）：'
        prop.parameterDefinitions.each { pd ->
            def type = pd.class.simpleName
            def marker = ''
            if (pd.name == 'BRANCH') {
                if (type == 'GitParameterDefinition') {
                    marker = '  ← 已经是下拉框 ✅'
                } else {
                    marker = '  ← 仍是文本输入框 ❌'
                    warn "BRANCH 参数类型是 ${type}，不是 GitParameterDefinition"
                    println '     原因：Jenkins 保留了上一次解析出的旧定义。'
                    println '     修复：先跑一次构建（或打开 Job 配置点保存），让 Jenkinsfile 重新注册参数。'
                    println '     若跑完仍是文本：确认 Git Parameter 插件已装，且 Jenkinsfile 里 useRepository 与仓库地址一致。'
                }
            }
            println "     - ${pd.name}: ${type}${marker}"
        }
    }
}

// ── 3. 创建 / 修复 ───────────────────────────────────────────────────
section(CFG.apply ? '执行修复' : '诊断模式')

if (!CFG.apply) {
    println '  当前 apply = false，未做任何修改。'
    println '  确认上面的结论后，把 CFG.apply 改成 true 再执行一次即可创建/修复 Job。'
} else {
    try {
        def scm = new GitSCM(
                [new UserRemoteConfig(CFG.repoUrl, null, null, CFG.credentialsId?.trim() ?: null)],
                [new BranchSpec(CFG.defaultBranch)],
                false, [], null, null, []
        )

        if (job == null) {
            job = jenkins.createProject(WorkflowJob, CFG.jobName)
            ok "已创建 Job：${CFG.jobName}"
        } else {
            info "更新已有 Job：${CFG.jobName}"
        }

        job.definition = new CpsScmFlowDefinition(scm, CFG.scriptPath)
        ok "已设置 SCM = ${CFG.repoUrl}"
        ok "已设置脚本路径 = ${CFG.scriptPath}"

        try {
            job.triggers.values().findAll { it instanceof SCMTrigger }.each { job.removeTrigger(it) }
            job.addTrigger(new SCMTrigger(CFG.pollSchedule))
            ok "已设置 SCM 轮询：${CFG.pollSchedule}"
        } catch (Throwable t) {
            warn "设置 SCM 轮询失败（可手动在 Job 配置里加）：${t.message}"
        }

        job.save()
        ok 'Job 已保存'
        println "\n  Job 地址：${jenkins.getRootUrl()}job/${CFG.jobName}/"
    } catch (Throwable t) {
        bad "创建/修复失败：${t.class.simpleName}: ${t.message}"
    }
}

// ── 4. 结论 ─────────────────────────────────────────────────────────
section '结论'

if (problems.isEmpty()) {
    println '  没有发现阻塞项。'
} else {
    println "  发现 ${problems.size} 个待处理项："
    problems.eachWithIndex { p, i -> println "   ${i + 1}. ${p}" }
}

println '''
  让 BRANCH 变成下拉框，按顺序确认三件事：
    ① Git Parameter 插件已安装并重启
    ② Jenkinsfile 的 gitParameter 里有 useRepository（= 仓库地址），
       私有仓库还要有 credentialId
    ③ Job 跑过一次构建（或配置页点保存），让 Jenkins 重新注册参数定义

  第 ③ 步最容易被忽略：Jenkins 会一直沿用上一次解析出来的参数定义，
  所以改完 Jenkinsfile 后第一次「Build with Parameters」看到的可能还是旧的输入框。
'''
