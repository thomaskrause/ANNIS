BEGIN; -- transaction
-----------
-- FACTS --
-----------

----- node

CREATE INDEX idx__node_id__:id
  ON facts_node_:id
  USING btree
  (id);

CREATE INDEX idx__node_is_token__:id
  ON facts_node_:id
  USING btree
  (is_token);

CREATE INDEX idx__node_left__:id
  ON facts_node_:id
  USING btree
  ("left");


CREATE INDEX idx__node_left_token__:id
  ON facts_node_:id
  USING btree
  (left_token);

CREATE INDEX idx__node_seg_name__:id
  ON facts_node_:id
  USING btree
  (seg_name);

CREATE INDEX idx__node_seg_index__:id
  ON facts_node_:id
  USING btree
  (seg_index);

CREATE INDEX idx__node_name__:id
  ON facts_node_:id
  USING btree
  ("name" varchar_pattern_ops);


CREATE INDEX idx__node_namespace__:id
  ON facts_node_:id
  USING btree
  (namespace varchar_pattern_ops);

CREATE INDEX idx__node_right__:id
  ON facts_node_:id
  USING btree
  ("right");


CREATE INDEX idx__node_right_token__:id
  ON facts_node_:id
  USING btree
  (right_token);

CREATE INDEX idx__node_span__:id
  ON facts_node_:id
  USING btree
  (span varchar_pattern_ops) WHERE span IS NOT NULL;


CREATE INDEX idx__node_token__:id
  ON facts_node_:id
  USING btree
  (token_index);

CREATE INDEX idx__node_corpus_ref__:id
  ON facts_node_:id
  USING btree
  (corpus_ref);

CREATE INDEX idx__node_text_ref__:id
  ON facts_node_:id
  USING btree
  (text_ref);

CREATE INDEX idx__node_rownum__:id
  ON facts_node_:id
  USING btree
  (n_na_rownum);

-- node_annotation
CREATE INDEX idx__node_annotation_val__:id
  ON facts_node_:id
  USING btree
  (val varchar_pattern_ops);

CREATE INDEX idx__node_annotation_val_ns__:id
  ON facts_node_:id
  USING btree
  (val_ns varchar_pattern_ops);


----- 2nd query
CREATE INDEX idx__2nd_query_:id ON facts_node_:id (corpus_ref, text_ref,left_token, right_token);

END; -- transaction
