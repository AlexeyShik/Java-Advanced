#!/bin/bash

cd ../

cp -a java-solutions solutions

javadoc -d api -private -link https://docs.oracle.com/en/java/javase/11/docs/api/ \
--module-path ../java-advanced-2021/lib:../java-advanced-2021/artifacts \
--module-source-path ../java-advanced-2021/modules:. \
solutions/info/kgeorgiy/ja/shik/implementor/*.java \
../java-advanced-2021/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/*Impler*.java

rm -rf solutions