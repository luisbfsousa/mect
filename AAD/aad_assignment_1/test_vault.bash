#! /bin/bash

set -e
vault_file=deti_coins_v2_vault.txt
while IFS= read -r line; do
  hash=$(echo "$line" | cut -b 5- | sha1sum | cut -b 1-40)
  echo "$line --- $hash"
done < ${vault_file}
