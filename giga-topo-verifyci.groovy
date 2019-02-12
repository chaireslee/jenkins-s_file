timestamps {

    // --------------配置项开始----------------------------
    // gerrit代码路径
    def gerritTriggerPattern = "OES/OES_iCOMPA_Analytics_UNM/topo"

    // gerrit代码URL
    def scmUrl = "ssh://5gzenapci@gerrit.zte.com.cn:29418/OES/OES_iCOMPA_Analytics_UNM/topo"

    // 以下两个配置适用于多个模块并存于一个三级目录的情况，对独占一个三级目录的模块以下两个配置应为空串
    // gerrit trigger上匹配代码路径，用于触发代码存放在四级目录的模块，对使用三级目录的模块应为空串
    def gerritFilePattern = ""
    // gerrit trigger上禁止匹配代码路径，用于触发代码存放在三级目录的模块，对使用四级目录的模块应为空串
    def gerritForbiddenFilePattern = ""

    // pom文件所在目录，对pom文件不在代码根目录或使用四级目录的模块必须配置，否则应为空串
    def pomDir = ""

    // Maven在10.96.33.27服务器上的路径和UT编译命令，请注意路径必须使用双反斜杠
    def mvnHome = "D:\\tools\\apache-maven-3.3.9"
    def mvnCommand = "clean install -Dmaven.test.skip=true"

    // KW项目名，为空串则不做KW检查
    def kwProj = ""

    // 代码开发主分支，非主分支代码不做KW检查
    def devMainBranch = "master"

    // JUnit报告文件，为空串则不发布JUnit报告
    def junitReportFile = ""

    // Cobertura报告文件，为空串则不发布Cobertura Coverage报告
    def coberturaReportFile = ""

    // 是否发布Jacoco报告，为空串则不发布Jacoco报告
    def jacocoReport = ""

    // 代码评审邮件接收列表
    def reviewMailList = "10062190@zte.com.cn, 00015006@zte.com.cn, 10145548@zte.com.cn, 10224474@zte.com.cn, 6407000756@zte.com.cn"

    // 代码verifyci失败邮件接收列表，代码责任人已默认添加
    def ciMailList = "10062190@zte.com.cn, 00015006@zte.com.cn, 10145548@zte.com.cn, 10224474@zte.com.cn, 6407000756@zte.com.cn"
    // --------------配置项结束----------------------------

    currentBuild.displayName = "#${BUILD_NUMBER} ${GERRIT_CHANGE_OWNER_NAME}"
    currentBuild.result = "SUCCESS"

    // 手工触发任务，则设置任务trigger属性后退出
    try {
        echo GERRIT_NAME
    }
    catch(err) {
        echo 'Need to set properties'
        if ( gerritFilePattern != "" ) {
            properties([
                buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')),
                pipelineTriggers([
                    gerrit(
                        customUrl: '', 
                        gerritProjects: [
                            [
                                branches: [[compareType: 'ANT', pattern: '**']], 
                                compareType: 'PLAIN', 
                                disableStrictForbiddenFileVerification: false, 
                                filePaths: [[compareType: 'ANT', pattern: gerritFilePattern]],
                                pattern: gerritTriggerPattern
                            ]
                        ], 
                        serverName: 'gerrit-server', 
                        triggerOnEvents: [
                            patchsetCreated(excludeDrafts: false, excludeNoCodeChange: false, excludeTrivialRebase: false), 
                            draftPublished(), 
                            commentAddedContains('recheck')
                        ]
                    )
                ])
            ])
        } else if ( gerritForbiddenFilePattern != "" ) {
            properties([
                buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')),
                pipelineTriggers([
                    gerrit(
                        customUrl: '', 
                        gerritProjects: [
                            [
                                branches: [[compareType: 'ANT', pattern: '**']], 
                                compareType: 'PLAIN', 
                                disableStrictForbiddenFileVerification: false, 
                                forbiddenFilePaths: [[compareType: 'ANT', pattern: gerritForbiddenFilePattern]],
                                pattern: gerritTriggerPattern
                            ]
                        ], 
                        serverName: 'gerrit-server', 
                        triggerOnEvents: [
                            patchsetCreated(excludeDrafts: false, excludeNoCodeChange: false, excludeTrivialRebase: false), 
                            draftPublished(), 
                            commentAddedContains('recheck')
                        ]
                    )
                ])
            ])
        } else {
            properties([
                buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')),
                pipelineTriggers([
                    gerrit(
                        customUrl: '', 
                        gerritProjects: [
                            [
                                branches: [[compareType: 'ANT', pattern: '**']], 
                                compareType: 'PLAIN', 
                                disableStrictForbiddenFileVerification: false, 
                                pattern: gerritTriggerPattern
                            ]
                        ], 
                        serverName: 'gerrit-server', 
                        triggerOnEvents: [
                            patchsetCreated(excludeDrafts: false, excludeNoCodeChange: false, excludeTrivialRebase: false), 
                            draftPublished(), 
                            commentAddedContains('recheck')
                        ]
                    )
                ])
            ])
        }
        return        
    }
    
    try {
        node('cloud_10.96.33.27') {
            stage('Checkout SCM') {
                echo 'Checkout SCM'
                // checkout两次，分别用于UT和KW
                checkout([
                    $class: 'GitSCM', 
                    branches: [
                        [name: '$GERRIT_REFSPEC']
                    ], 
                    doGenerateSubmoduleConfigurations: false, 
                    extensions: [
                        [
                            $class: 'BuildChooserSetting',
                            buildChooser: [
                                $class: 'GerritTriggerBuildChooser'
                            ]
                        ],
                        [
                            $class: 'RelativeTargetDirectory', 
                            relativeTargetDir: 'ut'
                        ]
                    ],
                    submoduleCfg: [], 
                    userRemoteConfigs: [
                        [
                            credentialsId: 'be404a55-a0fb-4797-ace2-92001d57b41f', 
                            refspec: 'refs/changes/*:refs/changes/*',
                            url: scmUrl
                        ]
                    ]
                ])

                checkout([
                    $class: 'GitSCM', 
                    branches: [
                        [name: '$GERRIT_REFSPEC']
                    ], 
                    doGenerateSubmoduleConfigurations: false, 
                    extensions: [
                        [
                            $class: 'BuildChooserSetting',
                            buildChooser: [
                                $class: 'GerritTriggerBuildChooser'
                            ]
                        ],
                        [
                            $class: 'RelativeTargetDirectory', 
                            relativeTargetDir: 'kw'
                        ]
                    ],
                    submoduleCfg: [], 
                    userRemoteConfigs: [
                        [
                            credentialsId: 'be404a55-a0fb-4797-ace2-92001d57b41f', 
                            refspec: 'refs/changes/*:refs/changes/*',
                            url: scmUrl
                        ]
                    ]
                ])

            }
            stage('Verify UT & KW') {
                parallel UT: {
                    echo 'Verify UT'
                    bat """
                        cd /d %WORKSPACE%\\ut\\${pomDir}
                        ${mvnHome}\\bin\\mvn ${mvnCommand}
                    """
                },
                KW: {
                    if ( kwProj != "" && env.GERRIT_BRANCH == devMainBranch ) {
                        echo 'Verify KW'
                        klocworkWrapper(installConfig: 'kw', ltoken: '', serverConfig: 'kw', serverProject: kwProj) {
                            bat """
                                cd /d %WORKSPACE%\\kw\\${pomDir}
                                ${mvnHome}\\bin\\mvn clean
                            """
                            bat """
                                cd /d %WORKSPACE%\\kw\\${pomDir}
                                set PATH=%PATH%;${mvnHome}\\bin
                                kwmaven --output %WORKSPACE%\\kw\\kwinject.out install
                                kwbuildproject -I --encoding UTF-8 --url http://localhost:9090/${kwProj} --tables-directory %WORKSPACE%\\kw\\kwtables %WORKSPACE%\\kw\\kwinject.out 
                                kwadmin --url http://localhost:9090/ load ${kwProj} %WORKSPACE%\\kw\\kwtables
                            """
                            klocworkQualityGateway([enableDesktopGateway: false, enableServerGateway: true, gatewayDesktopConfig: [reportFile: '', threshold: '1'], gatewayServerConfigs: [[conditionName: '', jobResult: 'failure', query: 'state:+New status:Analyze severity:1,2', threshold: '1']]])
                        }
                    } else {
                        echo 'Ignore Verify KW'
                    }
                }
            }
            stage('Results') {
                echo 'Results'
                if ( junitReportFile != "" ) {
                    junit testResults: junitReportFile, allowEmptyResults: true
                }
                if  ( coberturaReportFile != "" ) {
                    step([$class: 'CoberturaPublisher', autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: coberturaReportFile, failNoReports: false, failUnhealthy: false, failUnstable: false, maxNumberOfBuilds: 0, onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false])
                }
                if  ( jacocoReport != "" ) {
                    step([$class: 'JacocoPublisher'])
                }
            }
        }
    }
    catch(err) {
        currentBuild.result = "FAILURE"
        throw err
    }
    finally {
        if(currentBuild.result == "SUCCESS") {
            node('cloud_10.96.33.27') {
                emailext(
                    to: reviewMailList,
                    subject: "【代码评审邀请】 ${env.GERRIT_CHANGE_OWNER_NAME} [${env.GERRIT_BRANCH}]${env.GERRIT_CHANGE_SUBJECT}",
                    body: """
                    <b>Project: </b>${env.GERRIT_PROJECT}<br/>
                    <b>Branch: </b>${env.GERRIT_BRANCH}<br/>
                    <b>Change Url: </b><a href=\"${env.GERRIT_CHANGE_URL}\">${env.GERRIT_CHANGE_URL}</a><br/>
                    <br/>
                    """,
                    mimeType: 'text/html',
                    attachLog: false
                )
            }
        }
        else {
            node('cloud_10.96.33.27') {
                def resultTitle = currentBuild.result == 'FAILURE' ? '失败' : '不稳定'
                emailext(
                    to: "${env.GERRIT_CHANGE_OWNER_EMAIL}, ${env.GERRIT_EVENT_ACCOUNT_EMAIL}, ${ciMailList}",
                    subject: "【${resultTitle}：${env.JOB_NAME}】 ${env.GERRIT_CHANGE_OWNER_NAME} [${env.GERRIT_BRANCH}]${env.GERRIT_CHANGE_SUBJECT}",
                    body: """
                    <b>Project: </b>${env.GERRIT_PROJECT}<br/>
                    <b>Branch: </b>${env.GERRIT_BRANCH}<br/>
                    <b>Change Url: </b><a href=\"${env.GERRIT_CHANGE_URL}\">${env.GERRIT_CHANGE_URL}</a><br/>
                    <b>Build Url: </b><a href=\"${env.JENKINS_URL}blue/organizations/jenkins/${env.JOB_NAME}/detail/${env.JOB_NAME}/${BUILD_NUMBER}/pipeline\">Click To View</a><br/>
                    <br/>
                    """,
                    mimeType: 'text/html',
                    attachLog: false
                )
            }
        }
    }
}