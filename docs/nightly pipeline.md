# Nightly & Release pipelines

* [Nightly & Release pipelines](#nightly--release-pipelines)
  * [Nightly pipeline](#nightly-pipeline)
    * [Nightly pipeline Architecture](#nightly-pipeline-architecture)
    * [Activate/Deactivate release branch](#activatedeactivate-release-branch)
    * [Nightly pipeline Parameters](#nightly-pipeline-parameters)
    * [Nightly pipeline Troubleshooting](#nightly-pipeline-troubleshooting)
      * [Nightly pipeline is failing](#nightly-pipeline-is-failing)
      * [Nightly pipeline is unstable](#nightly-pipeline-is-unstable)
      * [Nightly Build & Deploy job is failing](#nightly-build--deploy-job-is-failing)
      * [Nightly Promote job is failing](#nightly-promote-job-is-failing)
    * [Testing the Nightly Pipeline](#testing-the-nightly-pipeline)
      * [Create specific Maven repository for nightly testing](#create-specific-maven-repository-for-nightly-testing)
      * [Create the different Jenkins jobs for nightly testing](#create-the-different-jenkins-jobs-for-nightly-testing)
      * [Launch a nightly with minimal parameters for nightly testing](#launch-a-nightly-with-minimal-parameters-for-nightly-testing)

In order to perform, Nightly and Release pipelines need to call some deploy and promote jobs for runtimes, examples, images and operator.  
Those jobs should be present at the same level as the nightly and/or release job, so they can be found when called.

Here is the list of jobs and link to Jenkinsfiles:

* [kogito-runtimes-deploy](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.deploy)
* [kogito-runtimes-promote](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.promote)
* [optaplanner-deploy](https://github.com/kiegroup/optaplanner/blob/master/Jenkinsfile.deploy)
* [optaplanner-promote](https://github.com/kiegroup/optaplanner/blob/master/Jenkinsfile.promote)
* [kogito-examples-deploy](https://github.com/kiegroup/kogito-examples/blob/master/Jenkinsfile.deploy)
* [kogito-examples-promote](https://github.com/kiegroup/kogito-examples/blob/master/Jenkinsfile.promote)
* [kogito-images-deploy](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.deploy)
* [kogito-images-promote](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.promote)
* [kogito-operator-deploy](https://github.com/kiegroup/kogito-operator/blob/master/Jenkinsfile.deploy)
* [kogito-operator-promote](https://github.com/kiegroup/kogito-operator/blob/master/Jenkinsfile.promote)

## Nightly pipeline

### Nightly pipeline Architecture

The Nightly Pipeline is composed of many steps, calling different other jobs to perform the build&test of runtimes/examples/images/operator as well as the deployment of jar artifacts and nightly container images.

**NOTE:** The Nightly Pipeline is a multibranch pipeline job and runs on `master` and each active release branch (for example 0.15.x).

![Flow](./images/nightly-flow.png)

Steps could be separated into 2 parts:

* **Build & Deploy**  
  Composed of calls to `*-deploy` jobs, is reponsible to runtimes, examples, images and operator are ok.  
  Note that if any problem happens in a `deploy` job, then other deploy jobs are still run (they will take the previous stable artifacts/images) to run.
* **Promote**  
  Composed of calls to `*-promote` jobs, is responsible to deploy snapshots artifacts/nightly images to repositories/registries.

Note that for a particular part of the project (runtimes, examples, images or operator), if the `Build & Deploy` failed, then the `Promote` part is skipped.

### Activate/Deactivate release branch

Once a branch is created or another one should be deactivated, you just need to go the configuration of the [nightly job](https://rhba-jenkins.rhev-ci-vms.eng.rdu2.redhat.com/job/KIE/job/kogito/job/kogito-nightly/configure) and change the `include` or `exclude` configuration of Git.

*TIP: just mention the branches you need in `include` part and leave the `exclude` part empty.*

### Nightly pipeline Parameters

If needed, the Nightly pipeline can be restarted with `SKIP_TESTS` or `SKIP` specific part options.  
See the [Nightly Jenkinsfile](../Jenkinsfile.nightly) for more information on parameters.

### Nightly pipeline Troubleshooting

Here are some information in case the Nightly pipeline fails ...

In the Zulip kogito-ci stream, there should be a link to the failing job. Open it and then open the `Blue Ocean` view. This will be easier to detect what is failing.

#### Nightly pipeline is failing

In case the main pipeline is failing, this will be most likely a Groovy error.  
This can happen when changes have been made to the [Nightly Jenkinsfile](../Jenkinsfile.nightly).

The problem will need to be corrected on the corresponding branch and the pipeline could be restarted.

#### Nightly pipeline is unstable

This usually means that one or many of the called jobs is/are failing. List of unsuccessful jobs should be displayed into Zulip and can be reviewed into the Blue Ocean.

By clicking the failing stage and accessing the failing job, you should be able to review the problem.

#### Nightly Build & Deploy job is failing

In that case, identify the error (Groovy script error or test problem), correct it and relaunch the Nightly Pipeline if necessary.

Here are some problems which can occur on a `Build & Deploy` job:

* Groovy script error (Jenkinsfile.deploy has some problems)
* Compilation
* Test errors
* Deployment problem (credentials)

#### Nightly Promote job is failing

In that case, identify the error (Groovy script error or test problem), correct it and rebuild the failing job.

Here are some problems which can occur on a `Promote` job:

* Groovy script error (Jenkinsfile.deploy has some problems)
* Deployment problem (credentials)

### Testing the Nightly Pipeline

In order to test the full Nightly Pipeline, and in order to avoid any problem, you will need to change some env in [Jenkinsfile.nightly](../Jenkinsfile.nightly), create jobs in Jenkins and setup some credentials.

* Have a specific container registry and credentials registered with `push` rights on it
* Have a specific Maven repository to deploy jar artifacts
* Create your own seed job which will create all the needed other jobs for you

#### Create specific Maven repository for nightly testing

For deploying runtimes and examples artifacts, and to avoid any conflict with main repository on snapshot artifacts, you will need to provide a nexus repository to deploy the artifacts.

If don't have one already, you can create one with the [nexus-operator](https://github.com/m88i/nexus-operator).

**IMPORTANT:** We don't support yet specific user's credentials. Anonymous user needs to have push rights to it.

#### Create the different Jenkins jobs for nightly testing

See [Jenkins documentation](./jenkins.md) to create the different jobs for the testing and adapt the configuration with correct `maven.artifacts_repository` and `cloud.*` properties.

**IMPORTANT: The different credentials IDs you are setting into the configuration should be available in Jenkins/Credentials.**

#### Launch a nightly with minimal parameters for nightly testing

Just Build with default parameters.

* (optional) `SKIP_TESTS` (usually you will want that)
* (optional) `SKIP_*` to skip different phases

**NOTE:** Deploy & Promote jobs of a specific repository can be ignored (and so job does not need to be created for testing), but you will need to check the corresponding `SKIP_` parameter.