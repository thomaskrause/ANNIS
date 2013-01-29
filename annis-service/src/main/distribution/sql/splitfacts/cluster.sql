CREATE INDEX idx__cluster__:id ON node_:id(span, corpus_ref, text_ref);
CLUSTER node_:id USING idx__cluster__:id;
DROP INDEX idx__cluster__:id;

CREATE INDEX idx__cluster__:id ON node_annotation_:id(val, corpus_ref, node_ref);
CLUSTER node_annotation_:id USING idx__cluster__:id;
DROP INDEX idx__cluster__:id;

CREATE INDEX idx__cluster__:id ON rank_:id(type_ref, pre, corpus_ref);
CLUSTER rank_:id USING idx__cluster__:id;
DROP INDEX idx__cluster__:id;

CREATE INDEX idx__cluster__:id ON edge_annotation_:id(rank_ref, val);
CLUSTER edge_annotation_:id USING idx__cluster__:id;
DROP INDEX idx__cluster__:id;