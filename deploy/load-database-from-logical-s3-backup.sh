#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Fetches a database backup produced by mysqldump (default yesterday's) from S3 and loads it into a database by means of 'mysql < dumpfile.sql'
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare weekday=${1:-$(env LANG=en_US date -d "yesterday 13:00" --utc +%A | tr [A-Z] [a-z])}
declare srcEnv=${2:-prod}
declare targetHost=${3:-localhost} #codekvast-default-staging.cahjor9xtqud.eu-central-1.rds.amazonaws.com

case ${weekday} in
  monday|tuesday|wednesday|thursday|friday|saturday|sunday|extra);;
  *) echo "Bad weekday: ${weekday}" >&1
     exit 1;;
esac

case ${srcEnv} in
  staging|prod) ;;
  *) echo "Bad srcEnv: ${srcEnv}" >&1
  exit 1;;
esac

if [[ -z $(which mysql) ]]; then
  echo "mysql is not installed" >&1
  exit 1
fi

echo -n "About to load the ${weekday} mysqldump from ${srcEnv} into jdbc:mariadb://${targetHost}:3306/codekvast. Continue [y/N]: "
read answer
case ${answer} in
    y|yes)
        echo "OK, here we go..."
        ;;
    *)
        echo "Nothing done."
        exit 1
        ;;
esac

declare s3_bucket="s3://io.codekvast.default.${srcEnv}.backup"
declare dumpfile=mysqldump-${weekday}.sql.gz

declare tmp_dir=$(mktemp -d /tmp/fetch-database.XXXXXXX)
trap "rm -fr ${tmp_dir}" EXIT

echo "Fetching ${s3_bucket}/${dumpfile} ..."
s3cmd get ${s3_bucket}/${dumpfile} ${tmp_dir}/${dumpfile}

echo "Loading ${srcEnv} ${dumpfile} into jdbc:mariadb://${targetHost}:3306/codekvast using the mysql client ..."
zcat ${tmp_dir}/${dumpfile} | mysql --protocol=tcp --user=codekvast --password=codekvast --host=${targetHost} --database=codekvast
