CREATE TABLE stage (
  id           VARCHAR PRIMARY KEY,
  name         VARCHAR,
  status       VARCHAR NOT NULL,
  create_date  TIMESTAMP NOT NULL,
  update_date  TIMESTAMP NOT NULL
);