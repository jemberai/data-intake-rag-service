-- connect to RDS database using master user
-- clear previous setup
DROP DATABASE IF EXISTS jember;
DROP USER IF EXISTS dataintakeowner;
DROP USER IF EXISTS dataintakeservice;
DROP ROLE IF EXISTS dataintake_role;

-- create database users. owner is the schema owner, service is the application user
CREATE USER dataintakeowner WITH PASSWORD 'changeme'; --change pwd
CREATE USER dataintakeservice WITH PASSWORD 'changeme'; --change pwd

-- create role and database
CREATE ROLE dataintake_role;
GRANT dataintake_role TO dataintakeowner;
GRANT dataintake_role TO postgres;
GRANT dataintake_role TO pg01;
CREATE DATABASE jember WITH OWNER dataintake_role;
-- run in pslq
\c jember;
CREATE SCHEMA dataintake AUTHORIZATION dataintakeowner;

ALTER USER dataintakeowner set SEARCH_PATH = dataintake;
ALTER USER dataintakeservice set SEARCH_PATH = dataintake;

ALTER ROLE dataintake_role IN DATABASE jember SET search_path TO dataintake;

CREATE ROLE dataintake_user;
GRANT dataintake_user TO dataintakeservice;
ALTER ROLE dataintake_user IN DATABASE jember SET search_path TO dataintake;

ALTER DEFAULT PRIVILEGES IN SCHEMA dataintake GRANT SELECT, INSERT, UPDATE, DELETE  ON TABLES TO dataintakeservice;
ALTER DEFAULT PRIVILEGES IN SCHEMA dataintake GRANT SELECT, INSERT, UPDATE, DELETE  ON TABLES TO dataintake_user;

-- alter any existing tables to have CRUD permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA dataintake TO dataintakeservice;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA dataintake TO dataintake_user;

