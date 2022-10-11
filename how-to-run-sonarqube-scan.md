#### How to run feature branche SonarQube scan 
1. Copy the name of the branch to be scanned
![Get branch name](../images/sonarqube-get-branch-name.jpg)

1. In a browser, open [jenkins-features test-framework-cucumber-framework job][1]
![Navigate jenkins-feature job](../images/sonarqube-jenkins-features-job.jpg)

1. Find the branch that will be scanned
![Branch to be scannes](../images/sonarqube-navigate-to-branch.jpg)

1. Trigger a run by clicking on Build Now (no parameters are needed)
![Trigger a run](../images/sonarqube-trigger-run.jpg)

1. Wait few minutes for the run to complete and click on the Analyzed by SonarQube icon
![Open SonarQube scan result](../images/sonarqube-navigate-to-project.jpg)

1. Navigate to the Issues tab and check the Severity filter. Make sure that no new blocker or critical issues are reported.
If there are any, resolve them. In case the code needs to remain this way, follow the [process described in confluence][2] to mark the issue as *Won't fix*
![Check the reported issues](../images/sonarqube-check-reported-issues.jpg)

1. Add the SonarQube branch result scan to your pull request

[1]: https://jenkins-features-mars.cd.genesaas.io/job/test-framework-cucumber-common/ "jenkins-features"
[2]: https://confluenceglobal.experian.local/confluence/pages/viewpage.action?pageId=345149843 "sonarqube-mark-issue-won't-fix" 