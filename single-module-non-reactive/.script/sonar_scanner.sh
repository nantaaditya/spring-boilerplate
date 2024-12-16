sonar-scanner -Dsonar.projectKey=boilerplate -Dsonar.projectName=boilerplate -Dsonar.token=${SONAR_TOKEN} -Dsonar.sources=./ -Dproject.settings=sonar-project.properties -Dsonar.scm.provider=git -Dsonar.scm.disabled=true -Dsonar.host.url=http://localhost:9000 -Dsonar.java.binaries=target -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml -Dsonar.junit.reportsPath=target/surefire-reports/TEST-*.xml -Dsonar.projectVersion=1.0.0-SNAPSHOT -Dsonar.qualitygate.wait=true
