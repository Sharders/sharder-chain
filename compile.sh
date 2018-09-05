#!/bin/sh

CP="lib/*:classes"
SP=src/java/

if [ -d "classes" ]; then
	rm -rf classes
fi

if [ -d "addons/classes" ]; then
	rm -rf addons/classes
fi

if [ ! -d "logs" ]; then
        mkdir logs
fi

mkdir -p classes
mkdir -p addons/classes/

echo "compiling sharder core & conch core..."
find src/java/org/conch/ -name "*.java" > sources.tmp
find src/java/org/sharder/ -name "*.java" >> sources.tmp
javac -encoding utf8 -sourcepath "${SP}" -classpath "${CP}" -d classes/ @sources.tmp || exit 1
echo "class files compiled successfully"
rm -f sources.tmp

find addons/src/ -name "*.java" > addons.tmp
if [ -s addons.tmp ]; then
    echo "compiling add-ons..."
    javac -encoding utf8 -sourcepath "${SP}:addons/src" -classpath "${CP}:addons/classes:addons/lib/*" -d addons/classes @addons.tmp || exit 1
    echo "add-ons compiled successfully"
    rm -f addons.tmp
else
    echo "no add-ons to compile"
    rm -f addons.tmp
fi