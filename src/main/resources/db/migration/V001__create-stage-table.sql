CREATE TABLE stage (
  id           BIGINT PRIMARY KEY,
  password     VARCHAR NOT NULL,
  name         VARCHAR,
  create_date  TIMESTAMP NOT NULL,
  update_date  TIMESTAMP NOT NULL
);