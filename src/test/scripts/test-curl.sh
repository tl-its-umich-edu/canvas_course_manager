#!/usr/bin/env bash
set -e
## Run curl queries to verify access to API manager and API.
## Get query and security settings from external file.
## This script is for test purposes and doesn't require much error handling.

## TTD (Things To Do)
## TTDM (Things To Do Maybe)
### - extract out the get token function to share with additional scripts (if more scripts are required).
### - print elapsed time for each query.
### - separate the security and query settings more clearly.
### - shift to yaml configuration.

source ./settings.sh

# This will hold current access token obtained by getAccessToken.
ACCESS_TOKEN=

set +x

function getAccessToken {

    # ask for a token
    AT=$(curl --request POST \
              -s \
              --url ${URL_PREFIX}/oauth2/token \
              --header 'accept: application/json' \
              --header 'content-type: application/x-www-form-urlencoded' \
              --data "grant_type=${GRANT_TYPE}&scope=${SCOPE}&client_id=${KEY}&client_secret=${SECRET}");
    
    # extract and squirrel the token away.
    ACCESS_TOKEN=$(echo ${AT} | perl -n -e'/access_token":"(.+)", "metadata.*/ && print "$1"' );

}

function getInstructorDetails {
    #set -x
    curl --request GET \
         --url "${URL_PREFIX}/Instructors/${UNIQUE_NAME}/Terms/${TERM_ID}/Classes" \
         --header 'accept: application/json' \
         --header "Authorization: Bearer ${ACCESS_TOKEN}" \
         --header "x-ibm-client-id: ${IBM_CLIENT_ID}"
}

##################################
getAccessToken
#echo "$ACCESS_TOKEN"

getInstructorDetails

