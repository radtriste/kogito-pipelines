import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class JenkinsfileSeed extends JenkinsPipelineSpecification {
	
	def '[Jenkinsfile.seed] test load script' () {
		when:
			def Jenkinsfile = loadPipelineScriptForTest('Jenkinsfile.seed.branch')
		then:
			Jenkinsfile != null
	}
}
