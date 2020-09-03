#!/bin/bash
.tools/gradle-2.14.1/bin/gradle wrapper
./gradlew build
.tools/gradle-2.14.1/bin/gradle assembleRelease
