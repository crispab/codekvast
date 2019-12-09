#!/usr/bin/env bash
#------------------------------------------------------------------------------------
# Send an event to Datadog if a JVM terminates with an abnormal exit code.
#
# It is invoked from a systemd.service file as ExecStopPost=/path/to/jvm-crash-notifier.sh app-name app-version
#
# It is invoked (by systemd.service) with the following environment:
# - $SERVICE_RESULT
# - $EXIT_CODE
# - $EXIT_STATUS
#------------------------------------------------------------------------------------
declare APP_NAME=$1
declare APP_VERSION=$2

declare DATADOG_API_KEY={{ datadog_api_key }}
declare DATADOG_APP_KEY={{ datadog_app_key }}
declare SLACK_WEBHOOK_TOKEN={{ codekvast_slackWebhookToken }}

printenv

echo "${APP_NAME} ${APP_VERSION} terminated with service result='${SERVICE_RESULT}', exit code=${EXIT_CODE} and exit status='${EXIT_STATUS}'"
