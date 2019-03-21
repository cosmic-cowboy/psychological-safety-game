CREATE TABLE round (
  id                  INTEGER PRIMARY KEY,
  stage_id            BIGINT NOT NULL,
  current_turn_number INTEGER NOT NULL,
  theme               VARCHAR
);