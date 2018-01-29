#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER stat WITH PASSWORD 'stat';
    CREATE SCHEMA stat AUTHORIZATION stat;
    ALTER USER stat WITH SUPERUSER;
EOSQL
