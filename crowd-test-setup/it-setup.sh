#!/bin/bash

set -euo pipefail

SELF_PATH="$(dirname "$(readlink -f "$0")")"

"$SELF_PATH/crowd/setup.sh"
"$SELF_PATH/crowd-init/setup.sh"

exit 0
