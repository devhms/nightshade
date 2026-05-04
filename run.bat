@echo off
.\apache-maven-3.9.6\bin\mvn.cmd exec:java -Dexec.mainClass="com.nightshade.CLI" -Dexec.args="-i sample-repo -s all -t 0.65 -v"
