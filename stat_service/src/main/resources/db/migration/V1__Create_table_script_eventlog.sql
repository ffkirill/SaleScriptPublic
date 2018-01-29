CREATE TABLE "script_eventlog" (
  "id"       BIGSERIAL PRIMARY KEY,
  "user_id" BIGINT NOT NULL,
  "script_id" BIGINT NOT NULL,
  "from_node" UUID NOT NULL,
  "to_node" UUID DEFAULT NULL,
  "reached_goal" BIGINT DEFAULT NULL,
  "text_from" TEXT DEFAULT NULL,
  "text_to" TEXT DEFAULT NULL,
  "timestamp" TIMESTAMP NOT NULL DEFAULT current_timestamp
);
