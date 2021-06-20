#!/bin/bash

CROWD_KEY="$1"
INSTALL_DIR="$PWD/tmp/crowd-it-test-installation"
CROWD_INSTALL="$INSTALL_DIR/atlassian-crowd"
CROWD_INSTANCE="$CROWD_INSTALL-instance"
CROWD_COOKIES="$INSTALL_DIR/cookies.txt"
CROWD_HOST="http://localhost:8095"

function wait_server_up {

  sleep 3
  until $(curl -L --silent --output /dev/null --head --fail "$CROWD_HOST/crowd/console"); do
    sleep 3
  done
}

function parse_token {

  echo "$1" | tr -d '[:space:]' | grep -o -P '(?<=name\=\"atl_token\"value\=\").+(?=\"id\=\"atl_token\")' | cut -d '"' -f1
}

function read_server_id {

  cat "$CROWD_INSTANCE/data/shared/crowd.cfg.xml" | grep -oPm1 '(?<=<property name="crowd.server.id">)[^<]+'
}

function getAtlToken {

  # fetch page to parse the token
  local RESULT=$(curl -L -X GET \
    --silent \
    --cookie "$CROWD_COOKIES" \
    "$CROWD_HOST""$1")

  # parse form token
  local ATL_TOKEN="$(parse_token "$RESULT")"

  echo $ATL_TOKEN
}

function get_directory_id {

  local UNIQUE_NAME="$1"

  # fetch page with information about initial directory
  local RESULT=$(curl -L -X POST \
    --silent \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/secure/directory/browse.action)" \
    --data-urlencode "active=" \
    --data-urlencode "name=$UNIQUE_NAME" \
    --data-urlencode "resultsPerPage=1" \
    "$CROWD_HOST"'/crowd/console/secure/directory/browse.action')

  # parse directory ID
  echo "$RESULT" | tr -d '[:space:]' | grep -o -P '(?<=data-id\=\")\d+(?=\"\>'"$UNIQUE_NAME"')'
}

function get_application_id {

  local UNIQUE_NAME="$1"

  # fetch page with information about initial directory
  local RESULT=$(curl -L -X GET \
    --silent \
    --cookie "$CROWD_COOKIES" \
    "$CROWD_HOST/crowd/console/secure/application/browse.action?name=$UNIQUE_NAME&active=&resultsPerPage=1")

  # parse directory ID
  echo "$RESULT" | tr -d '[:space:]' | grep -o -P '(?<=ID\=)\d+(?=\"\>'"$UNIQUE_NAME"')'
}

function login_web {

  # login user
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie-jar "$CROWD_COOKIES" \
    --header "Content-Type: application/json" \
    --header "X-Atlassian-Token: no-check" \
    --data '{"username":"'"$1"'","password":"'"$2"'","rememberMe":"false"}' \
    "$CROWD_HOST/crowd/rest/security/login?next=%2Fconsole"
}

function pre_config {

  local SERVER_ID="$(read_server_id)"
  local DIRECTORY="$1"
  local EMAIL="$2"
  local UNIQUE_NAME="$3"
  local FIRST_NAME="$4"
  local LAST_NAME="$5"
  local PASSWORD="$6"

  # fetch session cookie
  curl -L -X GET \
    --silent --output /dev/null \
    --cookie-jar "$CROWD_COOKIES" \
    "$CROWD_HOST/crowd/console"

  # set Crowd license key
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/setup/setuplicense.action)" \
    --data-urlencode "sid=$SERVER_ID" \
    --data-urlencode "key=$CROWD_KEY" \
    "$CROWD_HOST"'/crowd/console/setup/setuplicense!update.action'

  # set flag to continue with installation process from null
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/setup/installtype.action)" \
    --data-urlencode "installOption=install.new" \
    "$CROWD_HOST"'/crowd/console/setup/installtype!update.action'

  # set usage of an embedded database
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/setup/setupdatabase.action)" \
    --data-urlencode "databaseOption=db.embedded" \
    "$CROWD_HOST"'/crowd/console/setup/setupdatabase!update.action'

  # set Crowd options
  curl -L -X POST \
  --silent --output /dev/null \
  --cookie "$CROWD_COOKIES" \
  --header "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "atl_token=$(getAtlToken /crowd/console/setup/setupoptions.action)" \
  --data-urlencode "title=Test Instance" \
  --data-urlencode "sessionTime=30" \
  --data-urlencode "baseURL=$CROWD_HOST/crowd" \
  "$CROWD_HOST"'/crowd/console/setup/setupoptions!update.action'

  # set initial internal directory
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/setup/directoryinternalsetup.action)" \
    --data-urlencode "name=$DIRECTORY" \
    "$CROWD_HOST"'/crowd/console/setup/directoryinternalsetup!update.action'

  # set first user
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/setup/defaultadministrator.action)" \
    --data-urlencode "email=$EMAIL" \
    --data-urlencode "name=$UNIQUE_NAME" \
    --data-urlencode "firstname=$FIRST_NAME" \
    --data-urlencode "lastname=$LAST_NAME" \
    --data-urlencode "password=$PASSWORD" \
    --data-urlencode "passwordConfirm=$PASSWORD" \
    "$CROWD_HOST"'/crowd/console/setup/defaultadministrator!update.action'

  # set no migration flags
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/setup/integration.action)" \
    "$CROWD_HOST"'/crowd/console/setup/integration!update.action'
}

function create_user {

  local DIRECTORY="$1"
  local DIRECTORY_ID="$(get_directory_id "$DIRECTORY")"
  local EMAIL="$2"
  local UNIQUE_NAME="$3"
  local FIRST_NAME="$4"
  local LAST_NAME="$5"
  local DISPLAYNAME="$6"
  local PASSWORD="$7"

  # safe user
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/secure/user/add.action)" \
    --data-urlencode "directoryID=$DIRECTORY_ID" \
    --data-urlencode "active=true" \
    --data-urlencode "email=$EMAIL" \
    --data-urlencode "name=$UNIQUE_NAME" \
    --data-urlencode "firstname=$FIRST_NAME" \
    --data-urlencode "lastname=$LAST_NAME" \
    --data-urlencode "displayname=$DISPLAYNAME" \
    --data-urlencode "password=$PASSWORD" \
    --data-urlencode "passwordConfirm=$PASSWORD" \
    "$CROWD_HOST"'/crowd/console/secure/user/add!update.action'
}

function create_group {

  local DIRECTORY="$1"
  local DIRECTORY_ID="$(get_directory_id "$DIRECTORY")"
  local UNIQUE_NAME="$2"
  local DESCRIPTION="$3"

  # safe group
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/secure/group/add.action)" \
    --data-urlencode "directoryID=$DIRECTORY_ID" \
    --data-urlencode "active=true" \
    --data-urlencode "name=$UNIQUE_NAME" \
    --data-urlencode "description=$DESCRIPTION" \
    "$CROWD_HOST"'/crowd/console/secure/group/add!update.action'
}

function add_user_to_group {

  local DIRECTORY="$1"
  local DIRECTORY_ID="$(get_directory_id "$DIRECTORY")"
  local UNIQUE_NAME="$2"
  local MEMBER_UNIQUE_NAME="$3"

  # safe user-group-membership
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken "/crowd/console/secure/group/viewmembers.action?directoryID=$DIRECTORY_ID&groupName=$UNIQUE_NAME")" \
    --data-urlencode "selectedEntityNames=$MEMBER_UNIQUE_NAME" \
    "$CROWD_HOST"'/crowd/console/secure/group/updatemembers!addUsers.action?'"directoryID=$DIRECTORY_ID&entityName=$UNIQUE_NAME"
}

function add_group_to_group {

  local DIRECTORY="$1"
  local DIRECTORY_ID="$(get_directory_id "$DIRECTORY")"
  local UNIQUE_NAME="$2"
  local MEMBER_UNIQUE_NAME="$3"

  # safe group-group-membership
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken "/crowd/console/secure/group/viewmembers.action?directoryID=$DIRECTORY_ID&groupName=$UNIQUE_NAME")" \
    --data-urlencode "selectedEntityNames=$MEMBER_UNIQUE_NAME" \
    "$CROWD_HOST"'/crowd/console/secure/group/updatemembers!addGroups.action?'"directoryID=$DIRECTORY_ID&entityName=$UNIQUE_NAME"
}

function create_directory {

  local UNIQUE_NAME="$1"

  # safe directory part 1
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/secure/directory/create.action)" \
    --data-urlencode "directoryType=INTERNAL" \
    "$CROWD_HOST"'/crowd/console/secure/directory/selectType.action'

  # safe directory part 2
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/secure/directory/createinternal.action)" \
    --data-urlencode "active=true" \
    --data-urlencode "name=$UNIQUE_NAME" \
    --data-urlencode "description=" \
    --data-urlencode "passwordRegex=" \
    --data-urlencode "passwordComplexityMessage=" \
    --data-urlencode "passwordMaxAttempts=0" \
    --data-urlencode "passwordHistoryCount=0" \
    --data-urlencode "passwordMaxChangeTime=0" \
    --data-urlencode "passwordExpirationNotificationPeriods=" \
    --data-urlencode "userEncryptionMethod=atlassian-security" \
    --data-urlencode "useNestedGroups=true" \
    --data-urlencode "permissionGroupAdd=true" \
    --data-urlencode "permissionPrincipalAdd=true" \
    --data-urlencode "permissionGroupModify=true" \
    --data-urlencode "permissionPrincipalModify=true" \
    --data-urlencode "permissionGroupAttributeModify=true" \
    --data-urlencode "permissionPrincipalAttributeModify=true" \
    --data-urlencode "permissionGroupRemove=true" \
    --data-urlencode "permissionPrincipalRemove=true" \
    "$CROWD_HOST"'/crowd/console/secure/directory/createinternal!update.action'
}

function create_app {

  local UNIQUE_NAME="$1"
  local APP_URL="$2"
  local APP_IP="$3"
  local PASSWORD="$4"
  local DIRECTORIES="$5"

  # create array from comma separated string
  #readarray -td ',' DIR_ARRAY <<< "$DIRECTORIES"
  IFS=',' read -ra DIR_ARRAY <<< "$DIRECTORIES"
  for i in "${!DIR_ARRAY[@]}"; do DIR_ARRAY[$i]="$(echo "${DIR_ARRAY[$i]}" | tr -d '[:space:]')"; done
  for i in "${!DIR_ARRAY[@]}"; do DIR_ARRAY[$i]="$(get_directory_id "${DIR_ARRAY[$i]}")"; done

  # safe app part 1
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/secure/application/addapplicationdetails.action)" \
    --data-urlencode "applicationType=GENERIC_APPLICATION" \
    --data-urlencode "name=$UNIQUE_NAME" \
    --data-urlencode "description=" \
    --data-urlencode "password=$PASSWORD" \
    --data-urlencode "passwordConfirmation=$PASSWORD" \
    "$CROWD_HOST"'/crowd/console/secure/application/addapplicationdetails!completeStep.action'

  # safe app part 2
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/secure/application/addapplicationconnectiondetails.action)" \
    --data-urlencode "applicationURL=$APP_URL" \
    --data-urlencode "remoteIPAddress=$APP_IP" \
    "$CROWD_HOST"'/crowd/console/secure/application/addapplicationconnectiondetails!completeStep.action'

  # safe app part 3
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/secure/application/addapplicationdirectorydetails.action)" \
    $(for i in "${!DIR_ARRAY[@]}"; do echo "--data-urlencode selecteddirectories=${DIR_ARRAY[$i]}"; done) \
    "$CROWD_HOST"'/crowd/console/secure/application/addapplicationdirectorydetails!completeStep.action'

  # safe app part 4
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/secure/application/addapplicationauthorisationdetails.action)" \
    $(for i in "${!DIR_ARRAY[@]}"; do echo "--data-urlencode allowAllToAuthenticateForDirectory-${DIR_ARRAY[$i]}=true"; done) \
    "$CROWD_HOST"'/crowd/console/secure/application/addapplicationauthorisationdetails!completeStep.action'

  # safe app part 5
  curl -L -X POST \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "atl_token=$(getAtlToken /crowd/console/secure/application/addapplicationconfirmation.action)" \
    "$CROWD_HOST"'/crowd/console/secure/application/addapplicationconfirmation!completeStep.action'
}

function enable_app_aggregation {

  local UNIQUE_NAME="$1"

  # set aggregation flag
  curl -L -X PUT \
    --silent --output /dev/null \
    --cookie "$CROWD_COOKIES" \
    --header "Content-Type: application/json" \
    --header "X-Atlassian-Token: no-check" \
    --data '{"aggregateMemberships":true}' \
    "$CROWD_HOST/crowd/rest/admin/latest/application/$(get_application_id "$UNIQUE_NAME")"
}

wait_server_up

pre_config 'non-test-dir' 'admin@email.com' 'Admin' 'FirstNameOfAdmin' 'LastNameOfAdmin' 'password'

login_web 'Admin' 'password'

create_directory 'ldap-test-dir-1'
create_directory 'ldap-test-dir-2'
create_directory 'ldap-test-dir-3'
create_directory 'ldap-test-dir-4'

create_user 'ldap-test-dir-1' 'a.user@email.com' 'UserA' 'FirstNameOfUserA' 'LastNameOfUserA' 'DisplayNameOfUserA' 'pw-user-a'
create_user 'ldap-test-dir-1' 'd.user@email.com' 'UserD' 'FirstNameOfUserD' 'LastNameOfUserD' 'DisplayNameOfUserD' 'pw-user-d'
create_group 'ldap-test-dir-1' 'GroupA' 'Description of GroupA.'
create_group 'ldap-test-dir-1' 'GroupC' 'Description of GroupC.'
add_user_to_group 'ldap-test-dir-1' 'GroupA' 'UserA'
add_user_to_group 'ldap-test-dir-1' 'GroupA' 'UserD'
add_group_to_group 'ldap-test-dir-1' 'GroupC' 'GroupA'

create_user 'ldap-test-dir-2' 'a.user@email.com' 'UserA' 'FirstNameOfUserA' 'LastNameOfUserA' 'DisplayNameOfUserA' 'pw-user-a'
create_user 'ldap-test-dir-2' 'b.user@email.com' 'UserB' 'FirstNameOfUserB' 'LastNameOfUserB' 'DisplayNameOfUserB' 'pw-user-b'
create_group 'ldap-test-dir-2' 'GroupA' 'Description of GroupA.'
create_group 'ldap-test-dir-2' 'GroupB' 'Description of GroupB.'
add_user_to_group 'ldap-test-dir-2' 'GroupA' 'UserA'
add_user_to_group 'ldap-test-dir-2' 'GroupA' 'UserB'
add_user_to_group 'ldap-test-dir-2' 'GroupB' 'UserA'

create_user 'ldap-test-dir-3' 'b.user@email.com' 'UserB' 'FirstNameOfUserB' 'LastNameOfUserB' 'DisplayNameOfUserB' 'pw-user-b'
create_user 'ldap-test-dir-3' 'c.user@email.com' 'UserC' 'FirstNameOfUserC' 'LastNameOfUserC' 'DisplayNameOfUserC' 'pw-user-c'
create_group 'ldap-test-dir-3' 'GroupB' 'Description of GroupB.'
add_user_to_group 'ldap-test-dir-3' 'GroupB' 'UserB'
add_user_to_group 'ldap-test-dir-3' 'GroupB' 'UserC'

create_user 'ldap-test-dir-4' 'e.user@email.com' 'UserE' 'FirstNameOfUserE' 'LastNameOfUserE' 'DisplayNameOfUserE' 'pw-user-e'
create_user 'ldap-test-dir-4' 'd.user@email.com' 'UserD' 'FirstNameOfUserD' 'LastNameOfUserD' 'DisplayNameOfUserD' 'pw-user-d'
create_group 'ldap-test-dir-4' 'GroupC' 'Description of GroupC.'
create_group 'ldap-test-dir-4' 'GroupD' 'Description of GroupD.'
create_group 'ldap-test-dir-4' 'GroupE' 'Description of GroupE.'
add_user_to_group 'ldap-test-dir-4' 'GroupC' 'UserD'
add_user_to_group 'ldap-test-dir-4' 'GroupE' 'UserE'
add_group_to_group 'ldap-test-dir-4' 'GroupD' 'GroupC'
add_group_to_group 'ldap-test-dir-4' 'GroupE' 'GroupD'

create_app 'ldap-adapter' 'http://localhost' '127.0.0.1' 'password' 'ldap-test-dir-1,ldap-test-dir-2,ldap-test-dir-3,ldap-test-dir-4'
enable_app_aggregation 'ldap-adapter'
