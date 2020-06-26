#!/usr/bin/env bash
#------------------------------------------------------------------------------
# Pushes ../product/server/login/src/heroku/addon-manifest.json to Heroku after
# inserting the correct secrets.
#------------------------------------------------------------------------------

cd $(dirname $0)

declare addonManifestSrc=../product/server/login/src/heroku/addon-manifest.json
declare secrets=playbooks/vars/secrets.yml

if [[ ! -f ${addonManifestSrc} ]]; then
    echo "No such file: $addonManifestSrc" 1>&2
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

declare tmpManifest=$(pwd)/addon-manifest.json
trap "rm -f ${tmpManifest}" EXIT

echo "Fetching the old manifest..."
rm -f ${tmpManifest}
heroku addons:admin:manifest:pull codekvast

echo "Replacing old live manifest with ${addonManifestSrc} (injecting correct credentials) ..."
cat ${addonManifestSrc} | sed "s/herokuApiPassword/${herokuApiPassword}/; s/herokuApiSsoSalt/${herokuApiSsoSalt}/" > ${tmpManifest}

echo "Checking that the real API password appears in addon-manifest.json ..."
grep -q "\"$herokuApiPassword\"" ${tmpManifest} || {
    echo "Failed to insert real Heroku API password in addon-manifest.json" 1>&2
    exit 1
}

echo "Checking that the real SSO salt appears in addon-manifest.json ..."
grep -q "\"$herokuApiSsoSalt\"" ${tmpManifest} || {
    echo "Failed to insert real Heroku SSO salt in addon-manifest.json" 1>&2
    exit 1
}

echo "Diffing the resulting addon-manifest.json..."
heroku addons:admin:manifest:diff

echo
echo "All looks fine."

echo -n "Push addon-manifest.json to Heroku? (y/N) "; read answer
if [[ "$answer" != "y" ]]; then
    echo "Nothing done."
    return 0
fi

echo "Pushing new manifest to Heroku..."
heroku addons:admin:manifest:push

echo "Fetching the new live manifest..."
heroku addons:admin:manifest:pull codekvast

echo "Updating ${addonManifestSrc} (removing secrets) ..."
cat ${tmpManifest} | sed "s/${herokuApiPassword}/herokuApiPassword/; s/${herokuApiSsoSalt}/herokuApiSsoSalt/" > ${addonManifestSrc}
