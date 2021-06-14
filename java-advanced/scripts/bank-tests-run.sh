#!/bin/bash

if ! javac -d ../out --module-path ../lib --add-modules junit ../java-solutions/info/kgeorgiy/ja/shik/bank/*.java \
  ../java-solutions/info/kgeorgiy/ja/shik/bank/*/*.java; then
  exit 1
fi

if ! java -cp ../out --module-path ../lib --add-modules junit info.kgeorgiy.ja.shik.bank.tests.BankTests; then
  rm -rf ../out
  exit 1
else
  rm -rf ../out
  exit 0
fi