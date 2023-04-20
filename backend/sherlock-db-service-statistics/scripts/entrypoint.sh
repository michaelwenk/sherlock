#!/bin/bash --login
# The --login ensures the bash configuration is loaded,
# enabling Conda.

# Enable strict mode.
set -euo pipefail
# ... Run whatever commands ...

# Temporarily disable strict mode and activate conda:
set +euo pipefail
source /root/miniconda3/bin/activate
conda init bash
conda activate my-rdkit-env

# Re-enable strict mode:
set -euo pipefail

# exec the final command:
exec java -Xmx4G -jar sherlock-db-service-statistics-0.0.1-SNAPSHOT.jar