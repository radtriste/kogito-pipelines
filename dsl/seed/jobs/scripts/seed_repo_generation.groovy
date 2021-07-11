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
            dir("${SEED_REPO}/${SEED_FOLDER}") {
                println "[INFO] Generate jobs for branch ${seedBranch} and repo ${repoName}."
                if (util.isDebug()) {
                    println "[DEBUG] Environment properties:"
                    envProps.each {
                        println "[DEBUG] ${it.key} = ${it.value}"
                    }
                }
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

    // Override with data from environment
    cfg.git.branch = "${GIT_BRANCH}"
    cfg.git.author.name = "${GIT_AUTHOR}"

    return cfg
}

def getRepoEnvProperties() {
    Map envProperties = [:]
    fillEnvProperties(envProperties, '', getRepoConfig())
    return envProperties
}

void fillEnvProperties(Map envProperties, String envKeyPrefix, Map propsMap) {
    propsMap.each { key, value ->
        String newKey = generateEnvKey(envKeyPrefix, key)
        if (isDebug()) {
            println "[DEBUG] Setting key ${newKey}"
        }
        if (value instanceof Map) {
            fillEnvProperties(envProperties, newKey, value as Map)
        } else if (value instanceof List) {
            envProperties[newKey] = (value as List).join(',')
        } else {
            envProperties[newKey] = value
        }
    }
}

String generateEnvKey(String envKeyPrefix, String key) {
    return (envKeyPrefix ? "${envKeyPrefix}_${key}" : key).toUpperCase()
}

return this
