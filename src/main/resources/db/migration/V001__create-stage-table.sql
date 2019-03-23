CREATE TABLE stage (
  id           VARCHAR PRIMARY KEY,
  password     VARCHAR NOT NULL,
  name         VARCHAR,
  status       VARCHAR NOT NULL,
  create_date  TIMESTAMP NOT NULL,
  update_date  TIMESTAMP NOT NULL
);