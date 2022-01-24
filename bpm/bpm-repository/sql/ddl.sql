CREATE USER bpm_repository;
ALTER USER bpm_repository WITH ENCRYPTED password 'bpm_repository';
CREATE DATABASE bpm_repository WITH ENCODING='UTF8' OWNER=bpm_repository;

CREATE TABLE "bpm_models" (
                              "id" varchar(80) PRIMARY KEY,
                              "code" varchar(40) NOT NULL,
                              "name" varchar(128) NOT NULL,
                              "description" text,
                              "notation" varchar(4) NOT NULL,
                              "xml" text NOT NULL,
                              "updated_by" varchar(100) NOT NULL,
                              "updated_at" timestamp NOT NULL
);

