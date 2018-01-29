#!/usr/bin/env bash
FILENAME=pgdump_`cat /etc/hostname`_$(date -d "today" +"%Y%m%d%H%M").sql
export AWS_ACCESS_KEY_ID=AKIAJM5VIZ7L6V7LBLHA
export AWS_SECRET_ACCESS_KEY=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
export AWS_DEFAULT_REGION=eu-central-1
PGPASSWORD=postgres pg_dumpall -U postgres -l postgres  -h postgres -p 5432 > $FILENAME
aws s3 cp $FILENAME s3://salescript-backup
rm $FILENAME
