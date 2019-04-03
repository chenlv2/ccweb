#!/bin/bash
JDK_VERSION=`$JAVA_HOME/bin/java -version 2>&1`
JDK_VERSION=$(echo $JDK_VERSION | grep "1.8.0")
echo $JDK_VERSION
if [[ "$JDK_VERSION" != "" ]]
then
	if [ ! -f "${JAVA_HOME}/lib/tools.jar" ];then 
		cp tools.jar $JAVA_HOME/lib/tools.jar
		echo "Copy tools.jar to $JAVA_HOME/lib/"
	fi
	
	cp $JAVA_HOME/lib/tools.jar $JAVA_HOME/jre/lib/tools.jar
	echo "Copy tools.jar to $JAVA_HOME/jar/lib/"
	
	echo "Installtion finish!!!"
else
    echo "Please install JDK1.8 first!!!"
fi
read -p "Press any key to continue." var
