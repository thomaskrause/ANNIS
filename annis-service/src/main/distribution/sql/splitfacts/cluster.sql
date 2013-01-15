CREATE INDEX idx__cluster__:id ON facts_node_:id(corpus_ref, node_anno_nr, id);
CLUSTER facts_node_:id USING idx__cluster__:id;
DROP INDEX idx__cluster__:id;

CREATE INDEX idx__cluster__:id ON facts_edge_:id(component_id, edge_anno_nr, pre);
CLUSTER facts_edge_:id USING idx__cluster__:id;
DROP INDEX idx__cluster__:id;