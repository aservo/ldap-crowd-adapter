#!/bin/bash

"$PWD/crowd-test-setup/download.sh" "$CROWD_TEST_VERSION"
"$PWD/crowd-test-setup/start.sh"
"$PWD/crowd-test-setup/init-with-trial-license-key.sh"
