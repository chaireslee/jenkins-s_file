timestamps {

    // --------------配置项开始----------------------------
    //编译归属人
    currentBuild.displayName = "#${BUILD_NUMBER} ${GERRIT_CHANGE_OWNER_NAME}"
    // gerrit上代码路径
    def gerritTriggerPattern = "OES/OES_iCOMPA_Analytics_UNM/topo"

    // gerrit上代码URL
    def scmUrl = "ssh://5gzenapci@gerrit.zte.com.cn:29418/OES/OES_iCOMPA_Analytics_UNM/topo"

    // 以下两个配置适用于多个模块并存于一个三级目录的情况，对独占一个三级目录的模块以下两个配置应为空串
    // gerrit trigger上匹配代码路径，用于触发代码存放在四级目录的模块，对使用三级目录的模块应为空串
    def gerritFilePattern = ""
    // gerrit trigger上禁止匹配代码路径，用于触发代码存放在三级目录的模块，对使用四级目录的模块应为空串
    def gerritForbiddenFilePattern = ""

    // pom文件所在目录，对pom文件不在代码根目录或使用四级目录的模块必须配置，否则应为空
    def pomDir = ""

    // Maven在10.96.33.27服务器上的路径和UT编译命令，请注意路径必须使用双反斜杠
    def mvnHome = "D:\\tools\\apache-maven-3.3.9"
    def mvnCommand = "clean deploy -Dmaven.test.skip=true -Daoki.build.dev=true"

    // oki安装文件路径，该路径是以JOB工作目录为根目录的相对路径
    def installFile = "giga-topo-package/target/oki"

    // 部署环境节点名，为空串则不部署
    def deployNode = 'U32_10.74.156.42'
    // 服务名，安装包解压后的目录里应包含该名称
    def serviceName = 'giga-topo'
    // 部署服务的租户名
    def tenantName = 'ranoss'
    // 为避免deploy脚本执行完成后，服务尚未真正启动导致自动化测试失败，需等待一段时间，根据经验值填写
    def serviceStartDelay = 60

    // 自动化测试节点名，为空串则不执行自动化测试
    // def autotestNode = 'U32_10.74.156.101'
    def autotestNode = ''
    // 自动化测试用例执行路径，该路径是以JOB工作目录为根目录的相对路径，请注意路径必须使用双反斜杠
    def autotestPath = "modules\\cmdb-test\\cmdb-test-robotframework\\oes-cmdb\\testcase"
    // 自动化测试用例执行命令，该命令将在自动化测试用例执行路径下执行
    def autotestCommand = 'call pybot testcase.txt'
    // robot报告文件路径，该路径是以JOB工作目录为根目录的相对路径
    def robotReportDir = "modules/cmdb-test/cmdb-test-robotframework/oes-cmdb/testcase"

    // 代码mergeci失败邮件接收列表，代码责任人已默认添加
    def ciMailList = "10062190@zte.com.cn, 00015006@zte.com.cn, 10145548@zte.com.cn, 10224474@zte.com.cn, 6407000756@zte.com.cn"
    // --------------配置项结束----------------------------

	def multiJob = 0

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
                        serverName: 'gerrit-server-verfiy2', 
                        triggerOnEvents: [
                            commentAdded(commentAddedTriggerApprovalValue: '+2', verdictCategory: 'Code-Review')
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
                        serverName: 'gerrit-server-verfiy2', 
                        triggerOnEvents: [
                            commentAdded(commentAddedTriggerApprovalValue: '+2', verdictCategory: 'Code-Review')
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
                        serverName: 'gerrit-server-verfiy2', 
                        triggerOnEvents: [
                            commentAdded(commentAddedTriggerApprovalValue: '+2', verdictCategory: 'Code-Review')
                        ]
                    )
                ])
            ])
        }
        return        
    }

    try {
        currentBuild.result = "SUCCESS"
        node('cloud_10.96.33.27') {
            stage('Merge Code') {
                bat """
                    ssh -l 5gzenapci -p 29418 gerrit.zte.com.cn gerrit review --submit --project ${env.GERRIT_PROJECT} ${env.GERRIT_PATCHSET_REVISION}
                """
            }

            // 如果前面的任务还在执行，直接返回
            bat """
                setlocal enabledelayedexpansion

                curl -o temp.xml -s -X POST "https://cloudci.zte.com.cn/central-u32/view/PRD/job/%JOB_NAME%/buildTimeTrend"
                sed -i "s#Estimated#Estimated\\r\\n#g" temp.xml
                type temp.xml | grep Estimated | wc -l > list_tmp.txt
 
                if exist job_exist.txt del /F/Q job_exist.txt

                for /F %%i in (list_tmp.txt) do (
                    echo job number %%i    
                    IF %%i GTR 1 (
                        echo exist > job_exist.txt
                    )
                )

                del /F/ Q temp.xml
                del /F /Q list_tmp.txt
            """
            if ( fileExists('job_exist.txt') ) {
                multiJob = 1
                return
            }

            stage('Build Package') {
                echo 'Build Package'
                checkout([
                    $class: 'GitSCM', 
                    branches: [
                        [name: '*/master']
                    ], 
                    doGenerateSubmoduleConfigurations: false, 
                    submoduleCfg: [], 
                    userRemoteConfigs: [
                        [
                            credentialsId: 'be404a55-a0fb-4797-ace2-92001d57b41f', 
                            url: scmUrl
                        ]
                    ]
                ])
                bat """
                    cd /d %WORKSPACE%\\${pomDir}
                    ${mvnHome}\\bin\\mvn ${mvnCommand}
                """
                bat """
                    mkdir mergeci\\%BUILD_TAG%
                    move /Y %WORKSPACE%\\${installFile}\\*.* mergeci\\%BUILD_TAG%
                    d:\\tools\\jfrog rt u mergeci\\\\%BUILD_TAG%\\\\* netnumen_u32-snapshot-generic --url=https://artxa.zte.com.cn/artifactory --apikey=AKCp5bAiqgujCuUbhvGogsZavXc4Rw2dfUFBxRbhY4tsfdd61MaDXE8kTA6QwwaU3ebTMuCca --flat=false
                    move /Y mergeci\\%BUILD_TAG%\\*.* ${installFile}
                    rd /S /Q  mergeci\\%BUILD_TAG%
                """
            }
        }

        if ( multiJob == 1 ) {
            echo 'Multiple job runs, exit'
            return
        }
        if ( deployNode != "" ) {
            node(deployNode) {
                stage("Auto Deploy") {
                    deleteDir()
                    sh """
                        if [ ! -f "/jenkins/jfrog" ]; then
                            wget ftp://u32admin:admin-u32@10.75.8.172/AOBS/jfrog
                            chmod +x jfrog
                            mv jfrog /jenkins
                        fi 
                        /jenkins/jfrog rt dl netnumen_u32-snapshot-generic/mergeci/${BUILD_TAG}/ --url=https://artxa.zte.com.cn/artifactory --apikey=AKCp5aTkmXuxw8CaNctKvovWg5zspZCrPWNgATZkpr1JRTgsjTj21pc7Zw3dnjiHFffbRn4iD
                    """
                    sh """
                        cd $WORKSPACE/mergeci/${BUILD_TAG}
                        tar -xf *.tar.gz
						cd `ls -F | grep ${serviceName} | grep '/'`
            			chmod +x *.sh
		        		./install.sh
                    """

                    sh """
                        cd $WORKSPACE/mergeci/${BUILD_TAG}
                        chmod +x *.sh
                        ./deploy_service.sh ${tenantName}
                        sleep ${serviceStartDelay}
                    """
                }
            }
        }

        if ( autotestNode != "" ) {
            node(autotestNode) {
                stage("Auto Test") {
                    checkout([
                        $class: 'GitSCM', 
                        branches: [
                            [name: '*/master']
                        ], 
                        doGenerateSubmoduleConfigurations: false, 
                        submoduleCfg: [], 
                        userRemoteConfigs: [
                            [
                                credentialsId: 'be404a55-a0fb-4797-ace2-92001d57b41f', 
                                url: scmUrl
                            ]
                        ]
                    ])

                    bat """
                        cd /d %WORKSPACE%\\${autotestPath}
                        ${autotestCommand}
                    """
                    
                    if ( fileExists("${robotReportDir}/output.xml") ) {
                        step([
                            $class: 'RobotPublisher',
                            enableCache: true,
                            outputPath: "${robotReportDir}",
                            outputFileName: "output.xml",
                            reportFileName: "report.html",
                            logFileName: "log.html",
                            unstableThreshold: 0.0,
                            passThreshold: 0.0,
                            disableArchiveOutput: false,
                            onlyCritical: true,
                            otherFiles: ''
                        ])
                    } else {
                        error 'No output.xml file fount!'
                    }
               }
           }
       }
    }
    catch(err) {
        currentBuild.result = "FAILURE"
        node('cloud_10.96.33.27') {
            emailext(
                to: "${env.GERRIT_CHANGE_OWNER_EMAIL}, ${env.GERRIT_EVENT_ACCOUNT_EMAIL}, ${ciMailList}",
                subject: "【失败：${env.JOB_NAME}】 ${env.GERRIT_CHANGE_OWNER_NAME} [${env.GERRIT_BRANCH}]${env.GERRIT_CHANGE_SUBJECT}",
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
        throw err
    }
}