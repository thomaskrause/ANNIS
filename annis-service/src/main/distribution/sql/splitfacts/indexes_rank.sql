CREATE INDEX idx__rank_level_:type__:id
  ON rank_:id
  USING btree
  ("level") WHERE type_ref = :type;


CREATE INDEX idx__rank_node_ref_:type__:id
  ON rank_:id
  USING btree
  (node_ref, corpus_ref) WHERE type_ref = :type;


CREATE INDEX idx__rank_parent_:type__:id
  ON rank_:id
  USING btree
  (parent) WHERE type_ref = :type;


CREATE INDEX idx__rank_post_:type__:id
  ON rank_:id
  USING btree
  (post) WHERE type_ref = :type;


CREATE INDEX idx__rank_pre_:type__:id
  ON rank_:id
  USING btree
  (pre) WHERE type_ref = :type;

CREATE INDEX idx__rank_pre_post_:type__:id
  ON rank_:id
  USING btree
  (pre, post, node_ref, corpus_ref) WHERE type_ref = :type;

CREATE INDEX idx__rank_root_:type__:id
  ON rank_:id
  USING btree
  (root) WHERE type_ref = :type;
