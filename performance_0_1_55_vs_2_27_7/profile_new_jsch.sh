#!/bin/bash
# Profile new JSch with JDK Flight Recorder

export JAVA_HOME="/c/tool/jdk-25.0.1+8"
export PATH="$JAVA_HOME/bin:$PATH"
MAVEN_HOME="/c/tool/apache-maven-3.9.12"

echo "Profiling NEW JSch (com.github.mwiede/jsch 2.27.7)..."
echo "=================================================="

# Run with JFR profiling
"$MAVEN_HOME/bin/mvn" test \
  -Dtest=DownloadSpeedTest#execute_grep \
  -DargLine="-XX:StartFlightRecording=duration=30s,filename=jfr_new_jsch.jfr,settings=profile"

echo ""
echo "JFR recording saved to: jfr_new_jsch.jfr"
echo "Analyzing with jfr tool..."
echo ""

# Extract hotspot methods
jfr print --events jdk.ExecutionSample jfr_new_jsch.jfr > jfr_new_jsch_samples.txt

# Extract summary statistics
jfr summary jfr_new_jsch.jfr > jfr_new_jsch_summary.txt

echo "Analysis complete!"
echo "- Full samples: jfr_new_jsch_samples.txt"
echo "- Summary: jfr_new_jsch_summary.txt"
echo "- JFR file: jfr_new_jsch.jfr (open in JDK Mission Control for GUI)"
