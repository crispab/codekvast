#!/usr/bin/env bash

mkdir -p -m 777 _site
touch .jekyll-metadata
chmod 777 .jekyll-metadata

if [ $(docker ps -q -a -f name=jekyll | wc -l) -eq 0 ]; then
    docker run --name=jekyll --volume=$(pwd):/srv/jekyll -it -p 127.0.0.1:4000:4000 jekyll/jekyll:pages bash
elif [ $(docker ps -q -f name=jekyll | wc -l) -eq 0 ]; then
    docker start -a -i jekyll <&1
fi
