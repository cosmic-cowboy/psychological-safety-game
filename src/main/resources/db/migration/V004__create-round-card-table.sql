CREATE TABLE round_card (
  round_id    INTEGER NOT NULL,
  turn_number INTEGER NOT NULL,
  card_id     VARCHAR NOT NULL,
  word        VARCHAR,
  CONSTRAINT foreign_key_round_card_round_id FOREIGN KEY (round_id) REFERENCES round(id)
);