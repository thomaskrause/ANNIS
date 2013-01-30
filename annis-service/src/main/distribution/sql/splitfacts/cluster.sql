CREATE INDEX idx__cluster__:id ON facts_node_:id(n_na_rownum, corpus_ref, is_token);
CLUSTER facts_node_:id USING idx__cluster__:id;
DROP INDEX idx__cluster__:id;


CREATE INDEX idx__cluster__:id ON facts_edge_:id(r_ea_rownum, corpus_ref, pre);
CLUSTER facts_edge_:id USING idx__cluster__:id;
DROP INDEX idx__cluster__:id;
