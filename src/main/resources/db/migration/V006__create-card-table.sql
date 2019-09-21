CREATE TABLE card (
  id           VARCHAR PRIMARY KEY,
  type         VARCHAR NOT NULL,
  text         VARCHAR NOT NULL,
  create_date  TIMESTAMP NOT NULL,
  update_date  TIMESTAMP NOT NULL
);