# Plugin to delete the build workspace.

## Pipeline

There is a single [step](https://jenkins.io/doc/pipeline/steps/ws-cleanup) to be used whenever workspace is allocated. 

## Job DSL

Examples:

```groovy
job("foo") {
    wrappers {
        preBuildCleanup { // Clean before build
            includePattern('**/target/**')
            deleteDirectories()
            cleanupParameter('CLEANUP')
        }
    }
    publishers {
        cleanWs { // Clean after build
            cleanWhenAborted(true)
            cleanWhenFailure(true)
            cleanWhenNotBuilt(false)
            cleanWhenSuccess(true)
            cleanWhenUnstable(true)
            deleteDirs(true)
            notFailBuild(true)
            disableDeferredWipeout(true)
            patterns {
                pattern {
                    type('EXCLUDE')
                    pattern('.propsfile')
                }
                pattern {
                    type('INCLUDE')
                    pattern('.gitignore')
                }
            }
        }
    }
}
```

## Declarative pipeline

The step `cleanWs` is available for use in a declarative pipeline. When you want
to clean the workspace after the build you can add this step under a suitable
condition in the [post](https://www.jenkins.io/doc/book/pipeline/syntax/#post)
section of your pipeline. If you want to clean the workspace before the build
starts, you need to add some extra configuration to be able to clean before the
sources are checked out from SCM. See the examples below for details.

The snippet generator that is built into Jenkins can assist you with what
configuration options are available. Just click on the "Pipeline Syntax" button
in your pipeline job, and select `cleanWs` from the "Sample Step" drop-down.  

Examples:

```groovy
pipeline { 
    agent any
    options {
        // This is required if you want to clean before build
        skipDefaultCheckout(true)
    }
    stages {
        stage('Build') {
            steps {
                // Clean before build
                cleanWs()
                // We need to explicitly checkout from SCM here
                checkout scm
                echo "Building ${env.JOB_NAME}..."
            }
        }
    }
    post {
        // Clean after build
        always {
            cleanWs(cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true,
                    patterns: [[pattern: '.gitignore', type: 'INCLUDE'],
                               [pattern: '.propsfile', type: 'EXCLUDE']])
        }
    }
}
```

## Pre-pipeline

The plugin provides a build wrapper (*Delete workspace before build starts*) and a post build step (*Delete workspace when build is done*).  These steps will allow you to configure which files will be deleted and in what circumstances.  The post build step can also take the build status into account.

## Pattern

Files to be deleted are specified by pattern using Ant syntax. In select box you can choose if the pattern
is *include* pattern (if the file matches this pattern, file will be
removed) or *exclude* pattern (if file matches this pattern, file won't
be removed). If there is only exclude pattern, as include pattern will
be used "\***/**", i.e. delete everything, which means that everything
will be deleted except files matching exclude pattern. Patterns are
applied only on files. If you want to apply them also on directories,
check checkbox. Please note, that directory is deleted with all
content, so if the directory matches include pattern, everything in this
dir will be deleted no matter if some files in this dir matches exclude
pattern. For detail see [javadoc](http://www.docjar.org/docs/api/org/apache/tools/ant/DirectoryScanner.html)

When whole workspace is supposed to be deleted (no patterns, external
commands, etc.), the plugin delegate to the [Resource Disposer Plugin](https://wiki.jenkins.io/display/JENKINS/Resource+Disposer+Plugin)
to speed things up.

## Disable deferred wipeout method

When deferred wipeout is disabled the old implementation of filesystem
content deletion is used. If you want the same behavior as with deferred
wipeout, you have to set plugin attribute *deleteDirs* to *true* as
well. In pipeline job's script, you can do that like:

```groovy
cleanWs disableDeferredWipeout: true, deleteDirs: true
```

For developers of (cloud, for instance) it might be useful to be sure
deferred wipeout is never selected as a cleanup method. Therefore there
is a new feature introduced to do this, implemented via regular
NodeProperty which you can attach to any node via UI or via script like:

```java
Node.getNodeProperties().add(new DisableDeferredWipeoutNodeProperty());
```
