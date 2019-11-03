#!/usr/bin/env bash

output='./app/src/main/graphql/run/drop/app/schema.json'
valid="\033[0;32m✔\033[0m"
wrong="\033[0;31m✖\033[0m"
normal="\033[0m"
bold="\033[1m"
underline="\033[4m"

function print_usage
{
     printf "${bold}NAME${normal}\n"
     printf "\tdl_schema - download graphql schema\n\n"
     printf "${bold}SYNOPSIS${normal}\n"
     printf "\t${bold}$0${normal} [${underline}OPTION${normal}]... [-e ENDPOINT | --endpoint=ENDPOINT]\n\n"
     printf "${bold}DESCRIPTION${normal}\n"
     printf "\t${underline}$0${normal} download the schemas from the graphql server ENDPOINT.\n\n"
     printf "\t${bold}-e${normal} ENDPOINT, ${bold}--endpoint=${normal}ENDPOINT\n"
     printf "\t\tthe graphql server endpoint\n\n"
     printf "\t${bold}-v${normal}, ${bold}--verbose${normal}\n"
     printf "\t\tenable verbose\n\n"
     printf "\t${bold}--help${normal}\tdisplay this help and exit\n"
}

if [ $# -eq 0 ] ;then
    printf " $wrong Bad usage\n\n"
    printf " run \"$0 --help\"\n"
    exit 1
fi

for arg in $* ;do
    if [ "$arg" = "--help" ] ;then
        print_usage
        exit 0
    fi
     if [ "$arg" = "--verbose" ] || [ "$arg" = "-v" ] ;then
        verbose_mod=0
    fi
done

for arg in $* ;do
    if [ -z ${arg##*=*} ] && [ ${arg%%=*} = "--endpoint" ] && [ ! -z ${arg#*=} ] ;then
        endpoint=${arg#*=}
        break
    fi
done
args=("$@")
if [ -z "$endpoint" ] ;then
    i=0
    for arg in $* ;do
        if [ "$arg" = "-e" ] && [ ! -z ${args[$i + 1]} ] ;then
            endpoint=${args[$i + 1]}
            break
        fi
        let i++
    done
fi

if [ -z "$endpoint" ] ;then
    printf " $wrong Bad usage\n\n"
    printf " run \"$0 --help\"\n"
    exit 1
fi

if type apollo > /dev/null 2>&1 ;then
    printf " $valid Apollo check\n"
else
    printf " $wrong Apollo check\n\n"
    echo This script needs apollo cli, please install it
	echo npm i -g apollo
	echo https://github.com/apollographql/apollo-tooling
    exit 1
fi

if [ -z "$verbose_mod" ] ;then
    apollo client:download-schema "$output" --endpoint="$endpoint" 2> /dev/null
else
    apollo client:download-schema "$output" --endpoint="$endpoint"
fi
apollo_status=$?
if [ "$apollo_status" -ne 0 ] && [ -z "$verbose_mod" ] ;then
    printf "\n run with -v or --verbose\n"
fi

set -e

./gradlew clean
./gradlew cleanBuildCache
./gradlew build

exit 0
