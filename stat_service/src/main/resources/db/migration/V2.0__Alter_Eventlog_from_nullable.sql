ALTER TABLE script_eventlog ALTER "from_node" SET DEFAULT NULL;
ALTER TABLE script_eventlog ALTER "from_node" DROP NOT NULL;
