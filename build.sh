#!/bin/bash

quit(){
	cd $cwd
	echo "$1" >&2
	[ $# -ge 2 ] && exit "$2"
	exit 1
}

cwd=$PWD

cd $(dirname $0)
[ -f .apksigner ] || quit "Missing file: $PWD/.apksigner"

skipgit=false
while [ $# -gt 0 ]; do
	if [ "$1" = "--skip-git" ]; then
		skipgit=true
		shift
	fi
done

if ! $skipgit; then
	build=false

	git fetch --all
	git checkout master || quit 'Git checkout failed'
	[ "$(git rev-parse HEAD)" != "$(git rev-parse origin/master)" ] && build=true
	git reset --hard origin/master

	output="$(git rebase upstream/master)"
	[ $? = 0 ] || { git rebase --abort; quit 'Git rebase failed'; }
	[ "$output" = "Current branch master is up to date." ] || build=true

	$build || quit "$output" 0
fi

[ "$JAVA_HOME" ] || export JAVA_HOME='/usr/lib/jvm/java-8-openjdk-amd64'
[ "$ANDROID_HOME" ] || export ANDROID_HOME='/usr/lib/android-sdk'

./gradlew assembleRelease || quit 'Compilation failure'
cp .apksigner app/build/outputs/apk/release/
cd app/build/outputs/apk/release/
if [ ! -f app-release-unsigned.apk ]; then
	ls -lh
	quit 'Apk file was not created'
fi

[ -f app-release-aligned.apk ] && rm app-release-aligned.apk
[ -f phonograph.apk ] && rm phonograph.apk


zipalign -v -p 4 app-release-unsigned.apk app-release-aligned.apk || quit 'Zipalign failed'
apksigner sign --ks /home/oscar/.keystore.jks --out phonograph.apk app-release-aligned.apk <.apksigner || quit 'Apksigner failed'
mv phonograph.apk /srv/sftp/phonograph-$(date '+%d%m%y').apk

[ "$(git rev-parse HEAD)" != "$(git rev-parse origin/master)" ] && git push -f origin master

cd "$cwd"
