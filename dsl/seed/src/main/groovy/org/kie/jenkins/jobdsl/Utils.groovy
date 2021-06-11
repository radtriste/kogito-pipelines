
package org.kie.jenkins.jobdsl

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class Utils {

    static def deepCopyObject(def originalMap) {
        return new JsonSlurper().parseText(JsonOutput.toJson(originalMap))
    }

    static String createRepositoryUrl(String author, String repositoryName) {
        return "https://github.com/${author}/${repositoryName}.git"
    }

    static String createProjectUrl(String author, String repositoryName) {
        return "https://github.com/${author}/${repositoryName}/"
    }

    static def getBindingValue(def script, String key) {
        return script.getBinding()[key]
    }

    static boolean isMainBranch(def script) {
        return getBindingValue(script, 'GIT_BRANCH') == getBindingValue(script, 'GIT_MAIN_BRANCH')
    }

    static String getQuarkusLTSVersion(def script) {
        return getBindingValue(script, 'QUARKUS_LTS_VERSION')
    }

    static boolean areTriggersDisabled(def script) {
        return getBindingValue(script, 'DISABLE_TRIGGERS').toBoolean()
    }

    static String getNightlyFolder(def script) {
        return "${KogitoConstants.KOGITO_DSL_NIGHTLY_FOLDER}/${getBindingValue(script, 'JOB_BRANCH_FOLDER')}"
    }

    static String getBDDRuntimesFolder(def script) {
        return "${KogitoConstants.KOGITO_DSL_PULLREQUEST_FOLDER}/${KogitoConstants.KOGITO_DSL_RUNTIMES_BDD_FOLDER}"
    }

    static String getReleaseFolder(def script) {
        return "${KogitoConstants.KOGITO_DSL_RELEASE_FOLDER}/${getBindingValue(script, 'JOB_BRANCH_FOLDER')}"
    }

    static String getToolsFolder(def script) {
        return "${KogitoConstants.KOGITO_DSL_TOOLS_FOLDER}"
    }

    static void createFolderHierarchy(def script, String fullPath) {
        String folderPath = ''
        fullPath.split('/').findAll { it != '' }.each {
            folderPath = folderPath ? "${folderPath}/${it}" : it
            println "Create job folder ${folderPath}"
            script.folder(folderPath)
    }
}

}
