#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Creates a database dump in the local file system
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare srcEnv=${1:-prod}
declare targetDirectory=${2:-$HOME/Dropbox/Codekvast/db-dumps}
declare dumpFile=codekvast-${srcEnv}-$(date --utc +"%Y%m%d-%H%M%S").sql
declare username=root

case ${srcEnv} in
  staging|prod) ;;
  *) echo "Bad srcEnv: ${srcEnv}" >&1
  exit 1;;
esac

if [[ -z $(which mariadb-dump) ]]; then
  echo "mariadb-dump is not installed" >&1
  exit 1
fi

if [[ ! -d ${targetDirectory} ]]; then
  echo "Target directory $targetDirectory does not exist"
  exit 1
fi

declare host=db-${srcEnv}.codekvast.io
declare password=$(yq eval ".secrets.mariadb.${srcEnv}.root_password" playbooks/vars/secrets.yml )
declare startedAtSecond=$(date +"%s")
declare targetFile=${targetDirectory}/${dumpFile}
echo "mariadb-dump --host=${host} --user=${username} --password=XXX --single-transaction --databases codekvast > $targetFile"
mariadb-dump --host=${host} --user=${username} --password=${password} --single-transaction --databases codekvast > ${targetFile}
declare elapsedSeconds=$(( $(date +"%s") - $startedAtSecond))
declare dumpSize=$(( $(stat --format="%s" ${targetFile}) / 1000000 ))
echo "Dumped codekvast-${srcEnv} to $targetFile ($dumpSize MB) in $elapsedSeconds seconds"
