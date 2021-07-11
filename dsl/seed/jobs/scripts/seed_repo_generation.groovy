SEED_FOLDER = 'dsl/seed'

util = null

def generate() {
    node('kie-rhel7 && kie-mem4g') {
        stage('Initialize') {
            checkout scm

            dir("${SEED_REPO}") {
                checkout(githubscm.resolveRepository("${SEED_REPO}", "${SEED_AUTHOR}", "${SEED_BRANCH}", false))
                echo 'This is the generate repo seed jobs'

                util = load "${SEED_FOLDER}/jobs/scripts/util.groovy"
            }
        }

        stage('Prepare jobs') {
            repoConfig = getRepoConfig()
            sh """
                cp ${repoConfig.git.jenkins_config_path}/dsl/jobs.groovy ${SEED_REPO}/${SEED_FOLDER}/jobs
            """
        }

        stage('Test jobs') {
            dir("${SEED_REPO}/${SEED_FOLDER}") {
                try {
                    sh './gradlew clean test'
                } finally {
                    junit 'build/test-results/**/*.xml'
                    archiveArtifacts 'build/reports/**'
                }
            }
        }

        stage('Generate jobs') {
            def envProps = getRepoEnvProperties()
            envProps.put('JOB_BRANCH_FOLDER', "${GENERATION_BRANCH}")
            envProps.put('GIT_MAIN_BRANCH', "${GIT_MAIN_BRANCH}")

            envProps.each {
                echo "${it.key} = ${it.value}"
            }
            if (util.isDebug()) {
                println '[DEBUG] Modified environment properties:'
                envProps.each {
                    println "[DEBUG] ${it.key} = ${it.value}"
                }
            }
            dir("${SEED_REPO}/${SEED_FOLDER}") {
                println "[INFO] Generate jobs for branch ${GENERATION_BRANCH} and repo ${REPO_NAME}."
            jobDsl targets: "jobs/jobs.groovy",
                sandbox: false,
                ignoreExisting: false,
                ignoreMissingFiles: false,
                removedJobAction: 'IGNORE',
                removedViewAction: 'IGNORE',
                //removedConfigFilesAction: 'IGNORE',
                lookupStrategy: 'SEED_JOB',
                additionalClasspath: 'src/main/groovy',
                additionalParameters : envProps
            }
        }
    }
    }

def getRepoConfig() {
    def cfg = util.getRepoConfig("${REPO_NAME}", "${GENERATION_BRANCH}")

    String author = "${GIT_AUTHOR}"
    String branch = "${GIT_BRANCH}"

    // Override with data from environment
    cfg.git.branch = branch
    cfg.git.author.name = author

    if (util.isDebug()) {
        println '[DEBUG] Modified repo config:'
        println "[DEBUG] ${cfg}"
    }

    return cfg
}

def getRepoEnvProperties() {
    Map envProperties = [:]
    fillEnvProperties(envProperties, '', getRepoConfig())
    if (util.isDebug()) {
        println '[DEBUG] Environment properties:'
        envProperties.each {
            println "[DEBUG] ${it.key} = ${it.value}"
        }
    }
    return envProperties
}

void fillEnvProperties(Map envProperties, String envKeyPrefix, Map propsMap) {
    propsMap.each { it ->
        String newKey = generateEnvKey(envKeyPrefix, it.key)
        def value = it.value
        if (util.isDebug()) {
            println "[DEBUG] Setting key ${newKey} and value ${value}"
        }
        if (value instanceof Map) {
            fillEnvProperties(envProperties, newKey, value as Map)
        } else if (value instanceof List) {
            envProperties.put(newKey, (value as List).join(','))
        } else {
            envProperties.put(newKey, value)
        }
    }
}

String generateEnvKey(String envKeyPrefix, String key) {
    return (envKeyPrefix ? "${envKeyPrefix}_${key}" : key).toUpperCase()
}

return this
