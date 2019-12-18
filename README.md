# Plugin to delete the build workspace.

## Pre-pipeline

The plugin declared build wrapper (*Delete workspace before build
starts*) and post build step (*Delete workspace when build is done*).
They both permit configuring what and in what circumstances will be
deleted. The post build step can also take into account the build
status.

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
pattern.

For detail see [javadoc](http://www.docjar.org/docs/api/org/apache/tools/ant/DirectoryScanner.html)

When whole workspace is supposed to be deleted (no patterns, external
commands, etc.), the plugin delegate to the [Resource Disposer Plugin](https://wiki.jenkins.io/display/JENKINS/Resource+Disposer+Plugin)
to speed things up.

## Pipeline

There is a single [step](https://jenkins.io/doc/pipeline/steps/ws-cleanup) to be used whener workspace is allocated. 

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

## Disable deferred wipeout method

When deferred wipeout is disabled the old implementation of filesystem
content deletion is used. If you want the same behavior as with deferred
wipeout, you have to set plugin attribute *deleteDirs* to *true* as
well. In pipeline job's script, you can do that like:

    cleanWs disableDeferredWipeout: true, deleteDirs: true

For developers of (cloud, for instance) it might be useful to be sure
deferred wipeout is never selected as a cleanup method. Therefore there
is a new feature introduced to do this, implemented via regular
NodeProperty which you can attach to any node via UI or via script like:

    Node.getNodeProperties().add(new DisableDeferredWipeoutNodeProperty());
