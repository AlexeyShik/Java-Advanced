cd ..\

mkdir -Force solutions

cp -Force -Recurse java-solutions\* solutions

javac -d out `
--module-path "..\java-advanced-2021\lib;..\java-advanced-2021\artifacts" `
--module-source-path "..\java-advanced-2021\modules;." `
--module solutions

rm -Recurse solutions

New-Item -ItemType Directory -Force -Path artifacts

cd out\solutions

jar -c -f ..\..\artifacts\info.kgeorgiy.ja.shik.jar `
-m ..\..\scripts\MANIFEST.MF `
info\kgeorgiy\ja\shik\implementor module-info.class

cd ..\..\

rm -Recurse out