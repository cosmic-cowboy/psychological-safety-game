CREATE TABLE stage_member (
  stage_id     VARCHAR NOT NULL,
  user_id      VARCHAR NOT NULL,
  user_name    VARCHAR NOT NULL,
  UNIQUE(stage_id, user_id),
  CONSTRAINT foreign_key_stage_member_stage_id FOREIGN KEY (stage_id) REFERENCES stage(id)
);