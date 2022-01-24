CREATE USER camunda;
ALTER USER camunda WITH ENCRYPTED password 'camunda';
CREATE DATABASE camunda WITH ENCODING='UTF8' OWNER=camunda;
