CREATE TABLE stage_user_card (
  id       VARCHAR NOT NULL,
  stage_id VARCHAR NOT NULL,
  user_id  VARCHAR NOT NULL,
  card_id  VARCHAR NOT NULL,
  UNIQUE(stage_id, user_id, card_id),
  CONSTRAINT foreign_key_stage_user_cards_stage_id FOREIGN KEY (stage_id) REFERENCES stage(id)
);