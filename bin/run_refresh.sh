#!/bin/bash
export CLASSPATH=$APP_HOME/target/dflmngr.jar:$APP_HOME/target/dependency/*
java -classpath $CLASSPATH net.dflmngr.handlers.AflFixtureLoaderHandler
java -classpath $CLASSPATH net.dflmngr.handlers.DflRoundInfoCalculator
java -classpath $CLASSPATH net.dflmngr.handlers.StartRoundJobGenerator
java -classpath $CLASSPATH net.dflmngr.handlers.EndRoundJobGenerator
java -classpath $CLASSPATH net.dflmngr.handlers.ResultsJobGenerator


