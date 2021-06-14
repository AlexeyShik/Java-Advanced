#!/bin/bash

if ! javac -d ../out --module-path ../lib --add-modules junit \
  ../java-solutions/info/kgeorgiy/ja/shik/bank/*.java \
  ../java-solutions/info/kgeorgiy/ja/shik/bank/*/*.java; then
  exit 1
fi

if ! java -cp ../lib/junit-4.11.jar:../lib/hamcrest-core-1.3.jar:../out org.junit.runner.JUnitCore \
  info.kgeorgiy.ja.shik.bank.tests.AdvancedClientTest info.kgeorgiy.ja.shik.bank.tests.AdvancedBankTest; then
    rm -rf ../out
    exit 1
else
    rm -rf ../out
    exit 0
fi