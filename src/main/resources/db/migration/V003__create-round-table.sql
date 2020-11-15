CREATE TABLE round (
  id                   BIGINT PRIMARY KEY,
  stage_id             VARCHAR NOT NULL,
  current_round_number INTEGER NOT NULL,
  situation_card_id    VARCHAR NOT NULL,
  status               VARCHAR NOT NULL,
  create_date          TIMESTAMP NOT NULL,
  CONSTRAINT foreign_key_round_stage_id FOREIGN KEY (stage_id) REFERENCES stage(id)
);