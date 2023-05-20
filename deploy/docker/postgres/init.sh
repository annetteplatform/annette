#!/bin/bash

set -e
set -u

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
      CREATE DATABASE keycloak WITH ENCODING ='UTF8';
      CREATE DATABASE camunda WITH ENCODING ='UTF8';
      CREATE DATABASE bpm_repository WITH ENCODING ='UTF8';
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d keycloak </tmp/keycloak.sql

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d bpm_repository <<-EOSQL
	   alter table if exists "business_process_variables"
         drop constraint "business_process_variable_pk";
     drop table if exists "business_process_variables";
     alter table if exists "business_processes"
         drop constraint "business_process_pk";
     drop table if exists "business_processes";
     alter table if exists "data_schema_variables"
         drop constraint "data_schema_variable_pk";
     drop table if exists "data_schema_variables";
     alter table if exists "data_schemas"
         drop constraint "data_schema_pk";
     drop table if exists "data_schemas";
     alter table if exists "bpm_models"
         drop constraint "bpm_model_pk";
     drop table if exists "bpm_models";


     create table "bpm_models"
     (
         "id"          VARCHAR(80)  NOT NULL,
         "code"        VARCHAR(80)  NOT NULL,
         "name"        VARCHAR(128) NOT NULL,
         "description" TEXT         NOT NULL,
         "notation"    VARCHAR(4)   NOT NULL,
         "xml"         TEXT         NOT NULL,
         "updated_at"  TIMESTAMP    NOT NULL,
         "updated_by"  VARCHAR(100) NOT NULL
     );
     alter table "bpm_models"
         add constraint "bpm_model_pk" primary key ("id");
     create table "data_schemas"
     (
         "id"          VARCHAR(80)  NOT NULL,
         "name"        VARCHAR(128) NOT NULL,
         "description" TEXT         NOT NULL,
         "updated_at"  TIMESTAMP    NOT NULL,
         "updated_by"  VARCHAR(100) NOT NULL
     );
     alter table "data_schemas"
         add constraint "data_schema_pk" primary key ("id");
     create table "data_schema_variables"
     (
         "data_schema_id" VARCHAR(80)  NOT NULL,
         "variable_name"  VARCHAR(80)  NOT NULL,
         "name"           VARCHAR(128) NOT NULL,
         "caption"        VARCHAR(128) NOT NULL,
         "datatype"       VARCHAR(20)  NOT NULL,
         "default_value"  TEXT         NOT NULL
     );
     alter table "data_schema_variables"
         add constraint "data_schema_variable_pk" primary key ("data_schema_id", "variable_name");
     create table "business_processes"
     (
         "id"                      VARCHAR(80)  NOT NULL,
         "name"                    VARCHAR(128) NOT NULL,
         "description"             TEXT         NOT NULL,
         "bpm_model_id"            VARCHAR(80),
         "process_definition_type" VARCHAR(3)   NOT NULL,
         "process_definition"      VARCHAR(128) NOT NULL,
         "data_schema_id"          VARCHAR(80),
         "updated_at"              TIMESTAMP    NOT NULL,
         "updated_by"              VARCHAR(100) NOT NULL
     );
     alter table "business_processes"
         add constraint "business_process_pk" primary key ("id");
     create table "business_process_variables"
     (
         "business_process_id" VARCHAR(80)  NOT NULL,
         "variable_name"       VARCHAR(80)  NOT NULL,
         "name"                VARCHAR(128) NOT NULL,
         "caption"             VARCHAR(128) NOT NULL,
         "datatype"            VARCHAR(20)  NOT NULL,
         "default_value"       TEXT         NOT NULL
     );
     alter table "business_process_variables"
         add constraint "business_process_variable_pk" primary key ("business_process_id", "variable_name");
     alter table "data_schema_variables"
         add constraint "data_schema_variable_fk_data_schema" foreign key ("data_schema_id") references "data_schemas" ("id") on update RESTRICT on delete CASCADE;
     alter table "business_processes"
         add constraint "business_process_fk_bpm_model" foreign key ("bpm_model_id") references "bpm_models" ("id") on update RESTRICT on delete RESTRICT;
     alter table "business_processes"
         add constraint "business_process_fk_data_schema" foreign key ("data_schema_id") references "data_schemas" ("id") on update RESTRICT on delete RESTRICT;
     alter table "business_process_variables"
         add constraint "business_process_variable_fk_business_process" foreign key ("business_process_id") references "business_processes" ("id") on update RESTRICT on delete CASCADE;

EOSQL




