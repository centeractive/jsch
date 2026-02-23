#!/bin/bash
# Profile old JSch with JDK Flight Recorder

export JAVA_HOME="/c/tool/jdk-25.0.1+8"
export PATH="$JAVA_HOME/bin:$PATH"
MAVEN_HOME="/c/tool/apache-maven-3.9.12"

echo "Profiling OLD JSch (com.jcraft/jsch 0.1.55)..."
echo "==============================================="

cd ../../local_projects/jsch

# Run with JFR profiling
"$MAVEN_HOME/bin/mvn" test \
  -Dtest=DownloadSpeedProblemTest#execute_grep \
  -DargLine="-XX:StartFlightRecording=duration=30s,filename=jfr_old_jsch.jfr,settings=profile"

echo ""
echo "JFR recording saved to: jfr_old_jsch.jfr"
echo "Analyzing with jfr tool..."
echo ""

# Extract hotspot methods
jfr print --events jdk.ExecutionSample jfr_old_jsch.jfr > jfr_old_jsch_samples.txt

# Extract summary statistics
jfr summary jfr_old_jsch.jfr > jfr_old_jsch_summary.txt

# Copy results back to comparison directory
cp jfr_old_jsch*.* ../../jsch_work/github_projects/jsch/

echo "Analysis complete!"
echo "- Full samples: jfr_old_jsch_samples.txt"
echo "- Summary: jfr_old_jsch_summary.txt"
echo "- JFR file: jfr_old_jsch.jfr (open in JDK Mission Control for GUI)"
echo ""
echo "Files copied to: C:/work/retrospective/jsch_work/github_projects/jsch/"
