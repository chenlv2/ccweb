#!/bin/sh
# nohup java -jar ccweb-start-1.0.0-SNAPSHOT.jar -Dwatchdocker=true >aftask.log 2>&1 &
nohup java -Xms256m -Xmx1024m -Xss255k -Duser.timezone=GMT+08 -jar ccweb-start-1.0.0-SNAPSHOT.jar >ccweb.log 2>&1 &
echo $! > tpid
echo Start ccweb-start-1.0.0-SNAPSHOT.jar Success!
exit
