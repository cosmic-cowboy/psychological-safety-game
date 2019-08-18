CREATE TABLE round_retrospective (
  id          VARCHAR NOT NULL,
  round_id    BIGINT NOT NULL,
  user_id     VARCHAR NOT NULL,
  card_id     VARCHAR NOT NULL,
  answer      VARCHAR,
  create_date TIMESTAMP NOT NULL,
  UNIQUE(round_id, user_id, card_id),
  CONSTRAINT foreign_key_round_retrospective_round_id FOREIGN KEY (round_id) REFERENCES round(id)
);