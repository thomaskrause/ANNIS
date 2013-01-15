BEGIN; -- transaction

CREATE INDEX  ON _corpus (id);
CREATE INDEX ON _node (id);
CREATE INDEX ON _node (text_ref);
CREATE INDEX ON _node(id) WHERE token_index is not null;
CREATE INDEX ON _node_annotation (node_ref);
CREATE INDEX ON _component (id);
CREATE INDEX ON _component (type);
CREATE INDEX ON _rank (node_ref);
CREATE INDEX ON _rank (component_ref);
CREATE INDEX ON _rank (pre, component_ref);
CREATE INDEX ON _component ("type");
CREATE INDEX ON _edge_annotation (rank_ref);

END; -- transaction