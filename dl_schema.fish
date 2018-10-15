#!/usr/bin/env fish

set -g output ./app/src/main/graphql/schema.json

function find_option
	if contains -- $current_flag_short $argv
		or contains -- $current_flag $argv
		return 0
	end
	return 1
end

function find_value
	if contains -- $current_flag_short $argv
		set index (contains -i -- $current_flag_short $argv)
		set -g option_value $argv[(math "$index + 1")]
		if is_valid_value $option_value
			return 0
		else
			return 1
		end
	end
	for arg in $argv
		set split (string split = -- $arg)
		if test $split[1] = $current_flag
			and test $split[2]
			set -g option_value $split[2]
			return 0
		end
	end
  	return 1
end

function is_valid_value
	if not string length -- $argv > /dev/null
		return 1
	end
	if string trim -l -c - -- $argv > /dev/null
		return 1
	end
  	return 0
end

function print_valid
	set_color green
	printf ✔
	set_color normal
end

function print_wrong
	set_color red
	printf ✖
	set_color normal
end

function event_wrong_value --on-event wrong_value
	printf ' %s' (print_wrong) $argv\n
    set -g error 1
end

function print_usage
	set_color -o
	printf Usage\n
	set_color normal
	printf '%s [...OPTIONS] [-e ENDPOINT | --endpoint=ENDPOINT] [-t TOKEN | --token=TOKEN]\n' (status -f)
	set_color -o
	printf \nArguments\n
	set_color normal
	printf '\tENDPOINT\tthe graphql server endpoint\n'
	printf '\tTOKEN\t\tthe graphql server token\n'
	set_color -o
	printf \nOptions\n
	set_color normal
	printf -- '\t-h, --help\tshow help\n'
	printf -- '\t-v, --verbose\tenable verbose mod\n'
end

set -g current_flag_short "-h"
set -g current_flag "--help"
if find_option $argv
	printf "Helper for download the schema from a GraphQL endpoint.\n\n"
	print_usage
	exit 0
end

if type -q apollo
	printf ' %s' (print_valid) Apollo check\n
else
	printf ' %s' (print_wrong) Apollo check\n\n
	echo This script needs apollo cli, please install it
	echo npm i -g apollo
	echo https://github.com/apollographql/apollo-cli
	exit 1
end

set -g current_flag_short "-e"
set -g current_flag "--endpoint"
if find_value $argv
	set endpoint $option_value
else
	emit wrong_value "Endpoint check"
end
set -g current_flag_short "-t"
set -g current_flag "--token"
if find_value $argv
	set token $option_value
else
	emit wrong_value "Token check"
end
if test $error
	printf '\nFor help run "%s --help"\n' (status -f)
	exit 1
end
set -g current_flag_short "-v"
set -g current_flag "--verbose"
if find_option $argv
	apollo schema:download $output --endpoint=$endpoint --header="Authorization: Bearer $token"
else
	apollo schema:download $output --endpoint=$endpoint --header="Authorization: Bearer $token" ^ /dev/null
end
if test $status -ne 0
	and not find_option $argv
	printf '\nRun with -v or --verbose to show more details\n'
	exit 1
else if test $status -ne 0
	exit 1
else
	exit 0
end
