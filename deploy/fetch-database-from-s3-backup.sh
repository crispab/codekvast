#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Fetches a database backup (default yesterday's) from S3 to the Docker container codekvast_database
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare weekday=${1:-$(env LANG=en_US date -d "yesterday 13:00" --utc +%A | tr [A-Z] [a-z])}
declare srcEnv=${2:-prod}
declare appName=${3:-xtrabackup}
declare tarball=${appName}-${weekday}.tar.gz

echo -n "About to fetch the ${weekday} backup from ${srcEnv} by means of ${appName}. Continue [y/N]: "
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

declare mysql_datadir=~/.codekvast_database
declare s3_bucket="s3://io.codekvast.default.${srcEnv}.backup"

declare tmp_dir1=$(mktemp -d /tmp/fetch-database.XXXXXXX)
declare tmp_dir2=$(mktemp -d /tmp/fetch-database.XXXXXXX)
trap "rm -fr ${tmp_dir1} ${tmp_dir2}" EXIT

s3cmd get ${s3_bucket}/${tarball} ${tmp_dir1}

echo "Unpacking ${tmp_dir1}/${tarball} into ${tmp_dir2}/ ..."
tar xf ${tmp_dir1}/${tarball} -C ${tmp_dir2}

echo "Running ${appName} --prepare --target-dir=${tmp_dir2}/ ..."
${appName} --prepare --target-dir=${tmp_dir2}/

echo "docker stop codekvast_database"
docker stop codekvast_database

echo "Cleaning $mysql_datadir/*"
sudo rm -fr ${mysql_datadir}/
mkdir -p ${mysql_datadir}/

echo "Moving ${tmp_dir2}/ to ${mysql_datadir}/ ..."
mv ${tmp_dir2}/* ${mysql_datadir}/

echo "Changing ownership of ${mysql_datadir}/ ..."
sudo chown -R ${USER}:"$(id -gn ${USER})" ${mysql_datadir}

echo "Starting a temporary MariaDB container without grant tables..."
declare container=$(docker run -d -v ${mysql_datadir}:/var/lib/mysql -p 3306:3306 mariadb:10.0 --skip-grant-tables)

echo "Waiting for MariaDB to start..."
wait-on tcp:localhost:3306 -d 10000 -t 60000 || exit 1

echo "Resetting passwords..."
docker exec ${container} mysql -e "
    use mysql;
    update user set password=PASSWORD('root') where User='root';
    update user set password=PASSWORD('codekvast') where User='codekvast';
    update user set plugin='mysql_native_password';"

echo "Stopping and removing temporary container..."
docker stop ${container}
docker rm -v ${container}

cd ..
./gradlew :product:server:login:startMariadb
