#!/bin/bash

# Fix Gradle Daemon Issues
echo "Fixing Gradle daemon issues..."

# Stop all Gradle daemons
pkill -f gradle || true

# Remove lock files
rm -rf ~/.gradle/wrapper/dists/gradle-8.13-bin/*/gradle-8.13-bin.zip.lck 2>/dev/null
rm -rf ~/.gradle/daemon 2>/dev/null

# Clean project
rm -rf .gradle build app/build

# Re-download Gradle wrapper if needed
echo "Gradle cleanup complete. Try running: ./gradlew --version"
