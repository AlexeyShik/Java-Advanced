#!/bin/bash

cd ../

cp -a java-solutions solutions

javac -d out \
--module-path ../java-advanced-2021/lib:../java-advanced-2021/artifacts \
--module-source-path ../java-advanced-2021/modules:. \
--module solutions

rm -rf solutions

mkdir -p artifacts

cd out/solutions || exit

jar -c -f ../../artifacts/info.kgeorgiy.ja.shik.implementor.jar \
-m ../../scripts/MANIFEST.MF \
info/kgeorgiy/ja/shik/implementor module-info.class

cd ../../

rm -rf ./out