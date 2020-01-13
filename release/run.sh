#!/bin/sh
# nohup java -jar ccweb-start-1.0.0-SNAPSHOT.jar -Dwatchdocker=true >aftask.log 2>&1 &
nohup java -Xms1024m -Xmx2048m -Xss512k -Duser.timezone=GMT+08 -jar ccweb-start-1.0.0-SNAPSHOT.jar >ccweb.log 2>&1 &
echo $! > tpid
echo Start ccweb-start-1.0.0-SNAPSHOT.jar Success!
exit
