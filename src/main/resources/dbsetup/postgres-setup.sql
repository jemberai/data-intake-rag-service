-- connect to RDS database using master user
-- clear previous setup
DROP DATABASE IF EXISTS intakeevent;
DROP USER IF EXISTS intakeeventadmin;
DROP USER IF EXISTS intakeeventservice;
DROP ROLE IF EXISTS intakeevent_role;

-- create database users. admin is schema owner, service is application user
CREATE USER intakeeventadmin WITH PASSWORD 'password'; --change pwd
CREATE USER intakeeventservice WITH PASSWORD 'password'; --change pwd

-- create role and database
CREATE ROLE intakeevent_role;
GRANT intakeevent_role TO intakeeventadmin;
GRANT intakeevent_role TO postgres;
CREATE DATABASE intakeevent WITH OWNER intakeevent_role;


-- Change connection to intakeevent database using intakeeventadmin user
-- these will apply default CRUD permissions to all tables created in the public schema
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE  ON TABLES TO dataintakeservice;



-- alter any existing tables to have CRUD permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO dataintakeservice;


