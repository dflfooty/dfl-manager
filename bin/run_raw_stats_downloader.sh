#!/bin/bash
export CLASSPATH=$APP_HOME/target/dflmngr.jar:$APP_HOME/target/dependency/*
java -classpath $CLASSPATH net.dflmngr.handlers.RawStatsDownloaderHandler $@
