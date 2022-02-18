CREATE USER bpm_repository;
ALTER USER bpm_repository WITH ENCRYPTED password 'bpm_repository';
CREATE DATABASE bpm_repository WITH ENCODING ='UTF8' OWNER = bpm_repository;
