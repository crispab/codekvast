#!/usr/bin/env bash
#------------------------------------------------------------------------------
# Pushes ../product/warehouse/src/heroku/addon-manifest.json to Heroku after
# inserting the correct secrets.
#------------------------------------------------------------------------------

declare addonManifest=../product/warehouse/src/heroku/addon-manifest.json
declare herokuApiPassword=$(grep herokuApiPassword playbooks/vars/secrets.yml | cut -d: -f2 | xargs)
declare herokuApiSsoSalt=$(grep herokuApiSsoSalt playbooks/vars/secrets.yml | cut -d: -f2 | xargs)

declare tmpFile=$(mktemp)
trap "rm -f ${tmpFile}" EXIT

echo "Inserting proper secrets into ${addonManifest} ..."
cat ${addonManifest} | sed "s/herokuApiPassword/${herokuApiPassword}/; s/herokuApiSsoSalt/${herokuApiSsoSalt}/" > ${tmpFile}

echo "Testing addon-manifest.json..."
kensa -f ${tmpFile} test manifest

echo "Pushing addon-manifest.json..."
kensa -f ${tmpFile} push
