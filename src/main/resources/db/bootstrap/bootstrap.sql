-- Bootstrap de la base de datos: una vez por entorno, como administrador (en Supabase, el usuario postgres).
-- No es una migración de Flyway: crea los roles con sus contraseñas (secretos) y deja que spin_migrator cree el
-- esquema `app`, del que será dueño. Todo lo demás (tablas, permisos) lo hacen las migraciones. ADR-0015.
--
--   psql "<url de administrador>" -v migrator_password='...' -v app_password='...' -f bootstrap.sql
--
-- Desde el editor SQL de Supabase: sustituir :'migrator_password' y :'app_password' por las contraseñas entre comillas.

-- Dueño del esquema; solo lo usa Flyway (DB_MIGRATOR_USER).
CREATE ROLE spin_migrator LOGIN PASSWORD :'migrator_password';

-- Rol de ejecución de la API (DB_APP_USER): solo los permisos que conceden las migraciones.
CREATE ROLE spin_app LOGIN PASSWORD :'app_password';
ALTER ROLE spin_app SET statement_timeout = '5s';

DO $$
BEGIN
    EXECUTE format('GRANT CREATE ON DATABASE %I TO spin_migrator', current_database());
END
$$;
