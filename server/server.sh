#!/bin/bash
eol=$'[^\n]*' # needed for bash regex

coproc nc -k -I 1 -l -p 3000

while read -r input; do
  if [[ "$input" =~ ^p\ +[0-9]${eol} ]]; # "p 1234"
    then # prime
      primeResult=$(factor `echo "$input" | awk {'print $2'}` | awk {'print $NF'})
      printf "%s." "$primeResult"
  fi
done <&"${COPROC[0]}" >&"${COPROC[1]}"
kill "$COPROC_PID"
