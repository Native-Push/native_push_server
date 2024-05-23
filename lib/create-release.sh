#!/bin/bash

../gradlew generatePomFileForMavenJavaPublication
../gradlew jar
../gradlew javadocJar
../gradlew kotlinSourcesJar

[ ! -e build/com ] || rm -r build/com

mkdir -p build/com/opdehipt/native-push/"$1"

cp build/libs/lib-"$1".jar build/com/opdehipt/native-push/"$1"/native-push-"$1".jar
cp build/libs/lib-"$1"-javadoc.jar build/com/opdehipt/native-push/"$1"/native-push-"$1"-javadoc.jar
cp build/libs/lib-"$1"-sources.jar build/com/opdehipt/native-push/"$1"/native-push-"$1"-sources.jar
cp build/publications/mavenJava/pom-default.xml build/com/opdehipt/native-push/"$1"/native-push-"$1".pom

cd build/com/opdehipt/native-push/"$1" || exit

gpg -ab native-push-"$1".jar
gpg -ab native-push-"$1"-javadoc.jar
gpg -ab native-push-"$1"-sources.jar
gpg -ab native-push-"$1".pom

md5 -r native-push-"$1".jar | cut -d " " -f1 > native-push-"$1".jar.md5
md5 -r native-push-"$1"-javadoc.jar | cut -d " " -f1 > native-push-"$1"-javadoc.jar.md5
md5 -r native-push-"$1"-sources.jar | cut -d " " -f1 > native-push-"$1"-sources.jar.md5
md5 -r native-push-"$1".pom | cut -d " " -f1 > native-push-"$1".pom.md5

openssl sha1 native-push-"$1".jar | cut -d " " -f2 > native-push-"$1".jar.sha1
openssl sha1 native-push-"$1"-javadoc.jar | cut -d " " -f2 > native-push-"$1"-javadoc.jar.sha1
openssl sha1 native-push-"$1"-sources.jar | cut -d " " -f2 > native-push-"$1"-sources.jar.sha1
openssl sha1 native-push-"$1".pom | cut -d " " -f2 > native-push-"$1".pom.sha1

cd ../../../..

zip -r native-push-zip com