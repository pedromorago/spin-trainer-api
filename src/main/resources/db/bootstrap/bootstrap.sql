-- Database bootstrap: once per environment, as administrator (in Supabase, the postgres user).
-- It is not a Flyway migration: it creates the roles with their passwords (secrets) and lets spin_migrator create the
-- `app` schema, which it will own. Everything else (tables, permissions) is done by the migrations. ADR-0015.
--
--   psql "<administrator url>" -v migrator_password='...' -v app_password='...' -f bootstrap.sql
--
-- From the Supabase SQL editor: replace :'migrator_password' and :'app_password' with the passwords in quotes.

-- Schema owner; only used by Flyway (DB_MIGRATOR_USER).
CREATE ROLE spin_migrator LOGIN PASSWORD :'migrator_password';

-- Runtime role of the API (DB_APP_USER): only the permissions granted by the migrations.
CREATE ROLE spin_app LOGIN PASSWORD :'app_password';
ALTER ROLE spin_app SET statement_timeout = '5s';

DO $$
BEGIN
    EXECUTE format('GRANT CREATE ON DATABASE %I TO spin_migrator', current_database());
END
$$;
