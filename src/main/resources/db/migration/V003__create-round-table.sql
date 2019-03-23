CREATE TABLE round (
  id                  INTEGER PRIMARY KEY,
  stage_id            VARCHAR NOT NULL,
  current_turn_number INTEGER NOT NULL,
  theme               VARCHAR,
  CONSTRAINT foreign_key_round_stage_id FOREIGN KEY (stage_id) REFERENCES stage(id)
);