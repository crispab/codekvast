#!/usr/bin/env bash
#------------------------------------------------------------------------------
# Pushes ../product/server/login/src/heroku/addon-manifest.json to Heroku after
# inserting the correct secrets.
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

declare tmpFile=$(mktemp)
trap "rm -f ${tmpFile}" EXIT

echo "Inserting proper secrets into ${addonManifest} ..."
cat ${addonManifest} | sed "s/herokuApiPassword/${herokuApiPassword}/; s/herokuApiSsoSalt/${herokuApiSsoSalt}/" > ${tmpFile}

echo "Checking that the real API password appears in addon-manifest.json ..."
grep -q "\"$herokuApiPassword\"" ${tmpFile} || {
    echo "Failed to insert real Heroku API password in addon-manifest.json" 1>&2
    exit 1
}

echo "Checking that the real SSO salt appears in addon-manifest.json ..."
grep -q "\"$herokuApiSsoSalt\"" ${tmpFile} || {
    echo "Failed to insert real Heroku SSO salt in addon-manifest.json" 1>&2
    exit 1
}

echo "Testing the resulting addon-manifest.json..."
kensa -f ${tmpFile} test manifest

echo
echo "All looks fine."

echo -n "Push addon-manifest.json to Heroku? (y/N) "; read answer
if [[ "$answer" == "y" ]]; then
    echo "Ok, here we go..."
    kensa -f ${tmpFile} push
else
    echo "Nothing done."
fi
