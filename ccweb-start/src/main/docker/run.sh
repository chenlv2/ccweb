#!/bin/sh
# nohup java -jar bservice-task-0.0.1-SNAPSHOT.jar -Dwatchdocker=true >aftask.log 2>&1 &
nohup java -Xms255m -Xmx255m -Xss255k -Duser.timezone=GMT+08 -jar bservice-task-0.0.1-SNAPSHOT.jar >aftask.log 2>&1 &
echo $! > tpid
echo Start bservice-task-0.0.1-SNAPSHOT.jar Success!
exit
