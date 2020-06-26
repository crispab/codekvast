#!/usr/bin/env bash
#------------------------------------------------------------------------------
# Pulls the production manifest from Heroku and updates
# ../product/server/login/src/heroku/addon-manifest.json
# after replacing secrets with placeholders.
#------------------------------------------------------------------------------

cd $(dirname $0)

declare addonManifest=../product/server/login/src/heroku/addon-manifest.json
declare secrets=playbooks/vars/secrets.yml

if [[ ! -f ${addonManifest} ]]; then
    echo "No such file: $addonManifest" 1>&2
    exit 1
fi

if [[ ! -f ${secrets} ]]; then
    echo "No such file: $secrets" 1>&2
    exit 1
fi

declare herokuApiPassword=$(yq read ${secrets} secrets.codekvast.heroku.provision.api.password)
declare herokuApiSsoSalt=$(yq read ${secrets} secrets.codekvast.heroku.provision.api.ssoSalt)

if [[ -z "$herokuApiPassword" ]]; then
    echo "Cannot find secrets.codekvast.heroku.provision.api.password in ${secrets}" 1>&2
    exit 1
fi

if [[ -z "$herokuApiSsoSalt" ]]; then
    echo "Cannot find secrets.codekvast.heroku.provision.api.ssoSalt in ${secrets}" 1>&2
    exit 1
fi

set -e
echo "Checking the Heroku token..."
heroku auth:token

echo "Fetching the old manifest..."
declare tmpManifest=$(pwd)/addon-manifest.json
trap "rm -f ${tmpManifest}" EXIT
heroku addons:admin:manifest:pull codekvast

echo "Removing secrets and updating ${addonManifest} ..."
cat ${tmpManifest} | sed "s/${herokuApiPassword}/herokuApiPassword/; s/${herokuApiSsoSalt}/herokuApiSsoSalt/" > ${addonManifest}
