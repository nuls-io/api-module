#!/bin/bash

contractAddress=$1

scriptdir=$(cd `dirname $0`; pwd)
homedir=`dirname $scriptdir`

cd $homedir/contract/code

srcdir=$contractAddress/src
if [ ! -d $srcdir ];then
    mkdir -p $srcdir
fi

logdir=$homedir/contract/logs
if [ ! -d $logdir ];then
    mkdir -p $logdir
fi

unzip -nq $contractAddress.zip -d $srcdir
ant -f compile.xml -Ddest=$contractAddress -Ddeploy_home=$homedir >>$logdir/compile.log
jar -cvf ./$contractAddress/$contractAddress.jar -C ./$contractAddress/classes . 1>/dev/null

exit 0