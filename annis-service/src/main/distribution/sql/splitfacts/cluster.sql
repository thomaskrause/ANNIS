CREATE INDEX idx__cluster__:id ON node_:id(corpus_ref);
CLUSTER node_:id USING idx__cluster__:id;
DROP INDEX idx__cluster__:id;
