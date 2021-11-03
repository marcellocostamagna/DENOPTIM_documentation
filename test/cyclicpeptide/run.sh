#!/bin/bash
#
# Script lauching an evolutionary experiment aiming to desing cyclic peptides
# from a template that constraints the search to 6-member cyclopeptides.
#
#
# Usage:
#
# ./scriptName.sh [-r]
#
# Options:
# -r           remove previously existing workspace.
#

# Process arguments
overwrite=1
args=($@)
for ((i=0; i<$#; i++))
do
    arg=${args[$i]}
    case "$arg" in
        "-r") overwrite=0 ;;
    esac
done

# Setting the environment
export SHELL="/bin/bash"
export DENOPTIM_HOME="$(cd ../.. ; pwd)"
export javaDENOPTIM="java"
export DENOPTIMJarFiles="$DENOPTIM_HOME/build"
if [ ! -f "$DENOPTIMJarFiles/DenoptimGA.jar" ]
then
    echo "Cannot find  $DENOPTIMJarFiles/DenoptimGA.jar"
    echo "Trying under dist folder"
    if [ ! -f "$DENOPTIMJarFiles/dist/DenoptimGA.jar" ]
    then
       echo "ERROR! Cannot find  $DENOPTIMJarFiles/dist/DenoptimGA.jar"
       exit -1
    fi
    export DENOPTIMJarFiles="$DENOPTIM_HOME/build/dist"
fi
echo "Using DENOPTIM from $DENOPTIMJarFiles"

# Copy to tmp space
wDir="/tmp/denoptim_CycloPeptides"
if [ -d "$wDir" ]
then
if [ $overwrite -eq 0 ]
    then
        rm -fr "$wDir"
    else
        echo " "
        echo "ERROR! Old $wDir exists already! Remove it to run a new test."
        echo " "
        exit
    fi
fi
mkdir "$wDir"
if [ ! -d "$wDir" ]
then
    echo " "
    echo "ERROR! Unable to create working directory $wDir"
    echo " "
    exit
fi
echo "Copying file to $wDir"
cp -r * "$wDir"
cd "$wDir"

# Run DENOPTIM	
echo " "
echo "Starting DenoptimGA (ctrl+c to kill)"
echo " "
java -jar "$DENOPTIMJarFiles/DenoptimGA.jar" input_parameters

# Goodbye
echo "All done. See results under $wDir"
echo "Thanks for using DENOPTIM!"
