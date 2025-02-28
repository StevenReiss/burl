#! /bin/bash -f

SRD=$(dirname "${BASH_SOURCE[0]}")
pushd $SRD > /dev/null
SRD1=`pwd`
popd > /dev/null

LIB=$SRD/../../ivy/lib
echo check $LIB
if test -d $LIB; then
   echo use local lib
else
   LIB=/pro/ivy/lib
fi

CP=$LIB/jakarta.mail.jar:$LIB/jakarta.activation.jar:$SRD1/burl.jar
java -cp $CP edu.brown.cs.burl.burl.BurlMain -L burlserver.log -S -LD



