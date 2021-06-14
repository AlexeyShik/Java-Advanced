#!/bin/bash

sh jar-run.sh

cd ../

java -jar artifacts/info.kgeorgiy.ja.shik.implementor.jar \
 -jar info.kgeorgiy.java.advanced.implementor.ImplerException implementation.jar

rm -rf artifacts