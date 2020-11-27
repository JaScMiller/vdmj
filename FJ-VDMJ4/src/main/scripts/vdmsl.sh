#!/bin/bash
#####################################################################################
# Execute VDMJ jar with various options
#####################################################################################

# Change these to flip VDMJ version
MVERSION="4.4.1-SNAPSHOT"
PVERSION="4.4.1-P-SNAPSHOT"

# The Maven repository directory containing VDMJ jars
MAVENREPO=~/.m2/repository/com/fujitsu

# Location of the vdmj.properties file, if any
PROPDIR="$HOME/lib"

# Details for 64-bit Java
JAVA64="/usr/bin/java"
JAVA64_VMOPTS="-Xmx3000m -Xss1m -Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote"

function help()
{
    echo "Usage: $0 [--help|-?] [-P] [-A] <VDMJ options>"
    echo "-P use high precision VDMJ"
    echo "-A use annotation libraries and options"
    echo "Java options are $JAVA64 $JAVA64_VMOPTS"
    echo "VDMJ options are $VDMJ_OPTS"
    exit 0
}

function check()
{
    if [ ! -r "$1" ]
    then
	echo "Cannot read $1"
	exit 1
    fi
}


# Just warn if a later version is available in Maven
LATEST=$(ls $MAVENREPO/vdmj | grep "^[0-9].[0-9].[0-9]" | tail -1)

if [ "$MVERSION" != "$LATEST" ]
then
    echo "WARNING: Latest VDMJ version is $LATEST, not $MVERSION"
fi


# Chosen version defaults to "master"
VERSION=$MVERSION

if [ $# -eq 0 ]
then help
fi

# Process non-VDMJ options
while [ $# -gt 0 ]
do
    case "$1" in
	--help|-\?)
	    help
	    ;;
	-A)
	    ANNOTATIONS_VERSION=$VERSION
	    ;;
	-P)
	    VERSION=$PVERSION
	    ;;
	*)
	    VDMJ_OPTS="$VDMJ_OPTS $1"
    esac
    shift
done

# Locate the jars
VDMJ_JAR=$MAVENREPO/vdmj/${VERSION}/vdmj-${VERSION}.jar
STDLIB_JAR=$MAVENREPO/stdlib/${VERSION}/stdlib-${VERSION}.jar
check "$VDMJ_JAR"
check "$STDLIB_JAR"
CLASSPATH="$VDMJ_JAR:$PROPDIR"
VDMJ_OPTS="-path $STDLIB_JAR $VDMJ_OPTS"
MAIN="com.fujitsu.vdmj.VDMJ"

if [ $ANNOTATIONS_VERSION ]
then
    ANNOTATIONS_JAR=$MAVENREPO/annotations/${VERSION}/annotations-${VERSION}.jar
    check "$ANNOTATIONS_JAR"
    ANNOTATIONS2_JAR=$MAVENREPO/annotations2/${VERSION}/annotations2-${VERSION}.jar
    check "$ANNOTATIONS2_JAR"
    VDMJ_OPTS="$VDMJ_OPTS -annotations"
    VDMJ_VMOPTS="$VDMJ_VMOPTS -Dannotations.debug"
    CLASSPATH="$CLASSPATH:$ANNOTATIONS_JAR:$ANNOTATIONS2_JAR"
fi


# The dialect is based on $0, so hard-link this file as vdmsl, vdmpp and vdmrt.
DIALECT=$(basename $0)

# Keep rlwrap output in a separate folder
export RLWRAP_HOME=~/.vdmj

# Execute the JVM...
exec rlwrap "$JAVA64" $JAVA64_VMOPTS $VDMJ_VMOPTS -cp $CLASSPATH $MAIN -$DIALECT $VDMJ_OPTS "$@"

