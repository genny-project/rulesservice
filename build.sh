#!/bin/bash
mvn clean package -Dmaven.multiModuleProjectDirectory=.
mvn eclipse:eclipse
rm -Rf .vertx/*
