## Changelog

##### Release 0.37 (2018-12-17)

-   Java 11 support was introduced in this release. Older versions does not support it.

##### Release 0.35 (2018-10-11)

-   Introduce a way how to disable deferred Wipeout ([PR#37](https://github.com/jenkinsci/ws-cleanup-plugin/pull/37),
    [JENKINS-53740](https://issues.jenkins-ci.org/browse/JENKINS-53740)
    - Getting issue details... STATUS )

##### Release 0.34 (2017-07-17)

-   Avoid using stale channel in disposable what node goes offline ([PR#33](https://github.com/jenkinsci/ws-cleanup-plugin/pull/33))

##### Release 0.33 (2017-04-24)

-   Add pipeline friendly syntax ([PR#30](https://github.com/jenkinsci/ws-cleanup-plugin/pull/30))
-   Japanese localization ([PR#32](https://github.com/jenkinsci/ws-cleanup-plugin/pull/32))
-   Fix inclusion/exclusion pattern layout ([JENKINS-43179](https://issues.jenkins-ci.org/browse/JENKINS-43179)
    - Getting issue details... STATUS )

##### Release 0.32 (2016-11-01)

-   Do not leak temporary directories that plugin failed to clean ([JENKINS-24824](https://issues.jenkins-ci.org/browse/JENKINS-24824))

##### Release 0.30 (2016-06-24)

-   Pipeline support

##### Release 0.27 (2015-08-19)

-   Adjusted console logging
-   Added logging to track down [JENKINS-24824](https://issues.jenkins-ci.org/browse/JENKINS-24824)

##### Release 0.26 (2015-05-29)

-   Require minimal width for pattern labels ([JENKINS-27103](https://issues.jenkins-ci.org/browse/JENKINS-27103))
-   Do not log exceptions on stderr

##### Release 0.25 (2015-01-25)

-   Survives workspace rename failure during async delete ([JENKINS-26250](https://issues.jenkins-ci.org/browse/JENKINS-26250))

##### Release 0.24 (2014-09-09)

-   Workspace is deleted asynchronously ([JENKINS-20056](https://issues.jenkins-ci.org/browse/JENKINS-20056)
    / [pull #20](https://github.com/jenkinsci/ws-cleanup-plugin/pull/20))
-   Fixed some warnings and code style ([pull #19](https://github.com/jenkinsci/ws-cleanup-plugin/pull/19))

##### Release 0.23 (2014-08-26)

-   Proper escaping of file paths ([pull #17](https://github.com/jenkinsci/ws-cleanup-plugin/pull/17))
-   Delete workspace fails ([JENKINS-23693](https://issues.jenkins-ci.org/browse/JENKINS-23693))

##### Release 0.22 (2014-08-03)

-   Fixed env. var. expansion on nodes

##### Release 0.21 (2014-06-23)

-   Don't follow symlinks
    ([JENKINS-13444](https://issues.jenkins-ci.org/browse/JENKINS-13444))
-   WS cleanup fails when some pattern is specified
    ([JENKINS-23494](https://issues.jenkins-ci.org/browse/JENKINS-23494))
-   External delete cmd doesn't work properly
    ([JENKINS-23523](https://issues.jenkins-ci.org/browse/JENKINS-23523))

##### Release 0.20 (2014-02-01)

-   Fixed build status setting - if the workspace cleanup fails, build
    status is set to FAILURE ([pull #14](https://github.com/jenkinsci/ws-cleanup-plugin/pull/14))

##### Release 0.19 (2013-10-08)

-   Fixed problem with spaces in external cleanup command ([pull #13](https://github.com/jenkinsci/ws-cleanup-plugin/pull/13))

##### Release 0.18 (2013-09-15)

-   Fixed broken backward compatibility in pre-build cleanup
    ([JENKINS-19574](https://issues.jenkins-ci.org/browse/JENKINS-19574))

##### Release 0.17 (2013-09-11)

-   Allow to configure external program to do the cleanup ([pull #12](https://github.com/jenkinsci/ws-cleanup-plugin/pull/12))
-   Fixed German translation encoding ([pull #11](https://github.com/jenkinsci/ws-cleanup-plugin/pull/11))

##### Release 0.16 (2013-07-02)

-   Added ability to specify if the workspace should be wiped out in
    pre-build step via a job parameter ([pull #10](https://github.com/jenkinsci/ws-cleanup-plugin/pull/10))

##### Release 0.15 (2013-06-18)

-   Don't wait for previous build step to complete ([pull #9](https://github.com/jenkinsci/ws-cleanup-plugin/pull/9))

##### Release 0.14 (2013-05-14)

-   Fix backward compatibility issues ([JENKINS-17930](https://issues.jenkins-ci.org/browse/JENKINS-17930),[JENKINS-17940](https://issues.jenkins-ci.org/browse/JENKINS-17940))
-   Delete the workspace regardless on the job result by default ([JENKINS-17930](https://issues.jenkins-ci.org/browse/JENKINS-17930))
-   Fix configuration ([JENKINS-17761](https://issues.jenkins-ci.org/browse/JENKINS-17761))

##### Release 0.13 (2013-05-03)

-   Configure deleting of workspace based on build status ([pull #7](https://github.com/jenkinsci/ws-cleanup-plugin/pull/7))
-   German translation ([pull #6](https://github.com/jenkinsci/ws-cleanup-plugin/pull/6))

##### Release 0.12 (2013-02-16)

-   Retry delete 3 times in prebuild cleanup and add eventually error
    message into console log ([pull #5](https://github.com/jenkinsci/ws-cleanup-plugin/pull/5))

##### Release 0.11 (2013-01-23)

-   Added option not to fail the build if some error happens during the
    cleanup ([JENKINS-15236](https://issues.jenkins-ci.org/browse/JENKINS-15236))
-   Added option to cleanup matrix parent workspace ([JENKINS-14128](https://issues.jenkins-ci.org/browse/JENKINS-14128))

##### Release 0.10 (2012-07-17)

-   Fixed skipping the cleanup - skip only when this option is checked
    ([pull #4](https://github.com/jenkinsci/ws-cleanup-plugin/pull/4))

##### Release 0.9 (2012-07-15)

-   Added option to skip the cleanup when build fails ([pull#3](https://github.com/jenkinsci/ws-cleanup-plugin/pull/3))

##### Release 0.8 (2012-03-14)

-   Ensure, that ws cleanup is run after all other plugins ([JENKINS-12962](https://issues.jenkins-ci.org/browse/JENKINS-12962))

##### Release 0.7 (2011-12-07)

-   Check if workspace exists ([JENKINS-11998](https://issues.jenkins-ci.org/browse/JENKINS-11998))
-   Added possibility to delete also directories when delete pattern is specified ([JENKINS-11927](https://issues.jenkins-ci.org/browse/JENKINS-11927))
-   Added possibility to specify also exclude patterns ([JENKINS-11928](https://issues.jenkins-ci.org/browse/JENKINS-11928))
-   Added missing Pattern descriptor

##### Release 0.6 (2011-10-11)

-   Ws clean up should be the first or the last step in case of pre-build or post-build cleanup, respectively ([JENKINS-11210](https://issues.jenkins-ci.org/browse/JENKINS-11210))

##### Release 0.5 (2011-09-27)

-   Added possibility to delete only part of the workspace specified by ant dir scanner pattern.

##### Release 0.4 (2011-04-07)

-   Bug fix - checkbox for deleting workspace after the build was not showing up on the job config page

##### Release 0.3 (2011-03-02)

-   Fix to delete right workspace when "concurrent builds" option is in use

##### Release 0.2 (2011-02-28)

-   Add an option to delete workspace before build (requires Jenkins 1.399 or higher)

##### Release 0.1 (2011-02-10)

-   Initial release

