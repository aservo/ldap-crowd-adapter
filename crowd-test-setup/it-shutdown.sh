#!/bin/bash

set -euo pipefail

SELF_PATH="$(dirname "$(readlink -f "$0")")"

"$SELF_PATH/crowd/shutdown.sh"
"$SELF_PATH/crowd-init/shutdown.sh"

exit 0
