CREATE SCHEMA IF NOT EXISTS scala_play_sandbox;

CREATE TABLE IF NOT EXISTS scala_play_sandbox.importData (
  id serial primary key
  ,fileName varchar(100) NOT NULL
  ,dateCreated timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);