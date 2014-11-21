To load the jucm files for junit testing, find where your junit plugin test is creating the junit-workspace folder. Copy
the junit-workspace folder content in the submission into your local junit-workspace folder.

Set launch configuration to not clear the workspace before running:
- uncheck "Clear" in the Main tab
- uncheck "Clear the configuration area before launching" in the Configuration tab

Make sure your execution environment is JRE 1.7 and the JVM is 32-bit.