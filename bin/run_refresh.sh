#!/bin/bash
export CLASSPATH=$APP_HOME/target/dflmngr.jar:$APP_HOME/target/dependency/*
java -classpath $CLASSPATH net.dflmngr.handlers.AflFixtureLoaderHandler
java -classpath $CLASSPATH net.dflmngr.handlers.DflRoundInfoCalculatorHandler
java -classpath $CLASSPATH net.dflmngr.scheduler.generators.StartRoundJobGenerator
java -classpath $CLASSPATH net.dflmngr.scheduler.generators.EndRoundJobGenerator
java -classpath $CLASSPATH net.dflmngr.scheduler.generators.ResultsJobGenerator


