CREATE INDEX idx__rank_level_:type__:id
  ON facts_edge_:id
  USING btree
  ("level") WHERE type_ref = :type;


CREATE INDEX idx__rank_node_ref_:type__:id
  ON facts_edge_:id
  USING btree
  (node_ref, corpus_ref) WHERE type_ref = :type;


CREATE INDEX idx__rank_parent_:type__:id
  ON facts_edge_:id
  USING btree
  (parent) WHERE type_ref = :type;


CREATE INDEX idx__rank_post_:type__:id
  ON facts_edge_:id
  USING btree
  (post) WHERE type_ref = :type;


CREATE INDEX idx__rank_pre_:type__:id
  ON facts_edge_:id
  USING btree
  (pre) WHERE type_ref = :type;

CREATE INDEX idx__rank_pre_post_:type__:id
  ON facts_edge_:id
  USING btree
  (pre, post, node_ref, corpus_ref) WHERE type_ref = :type;

CREATE INDEX idx__rank_root_:type__:id
  ON facts_edge_:id
  USING btree
  (root) WHERE type_ref = :type;

-- edge_annotation
CREATE INDEX idx__edge_annotation_val_:type__:id
  ON facts_edge_:id
  USING btree
  (val varchar_pattern_ops) WHERE type_ref = :type;

CREATE INDEX idx__edge_annotation_val_ns_:type__:id
  ON facts_edge_:id
  USING btree
  (val_ns varchar_pattern_ops) WHERE type_ref = :type;

CREATE INDEX idx__edge_rownum_:type__:id
  ON facts_edge_:id
  USING btree
  (r_ea_rownum) WHERE type_ref = :type;
