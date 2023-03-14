#!/bin/bash
export CLASSPATH=$APP_HOME/target/dflmngr.jar:$APP_HOME/target/dependency/*
echo $CLASSPATH
java -classpath $CLASSPATH net.dflmngr.scheduler.generators.EmailSelectionsJobGenerator
