#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Creates a database dump in the local file system
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare srcEnv=${1:-prod}
declare targetDirectory=${2:-$HOME/Dropbox/Codekvast/db-dumps}
declare dumpFile=codekvast-${srcEnv}-$(date --utc +"%Y%m%d-%H%M%S").sql
declare username=root
declare password=$(grep mariadb_root_password playbooks/vars/secrets.yml | cut -d: -f2 | xargs)

case ${srcEnv} in
  staging|prod) ;;
  *) echo "Bad srcEnv: ${srcEnv}" >&1
  exit 1;;
esac

if [[ -z $(which mysqldump) ]]; then
  echo "mysqldump is not installed" >&1
  exit 1
fi

if [[ ! -d ${targetDirectory} ]]; then
  echo "Target directory $targetDirectory does not exist"
  exit 1
fi

declare host=codekvast-${srcEnv}.cahjor9xtqud.eu-central-1.rds.amazonaws.com

declare startedAtSecond=$(date +"%s")
declare targetFile=${targetDirectory}/${dumpFile}
echo "mysqldump --host=${host} --user=${username} --password=XXX --single-transaction --databases codekvast > $targetFile"
mysqldump --host=${host} --user=${username} --password=${password} --single-transaction --databases codekvast > ${targetFile}
declare elapsedSeconds=$(( $(date +"%s") - $startedAtSecond))
declare dumpSize=$(( $(stat --format="%s" ${targetFile}) / 1000000 ))
echo "Dumped codekvast-${srcEnv} to $targetFile ($dumpSize MB) in $elapsedSeconds seconds"
