BEGIN; -- transaction
-----------
-- FACTS --
-----------

----- node

CREATE INDEX idx__node_id__:id
  ON facts_node_:id
  USING btree
  (id) WITH(FILLFACTOR=100);

CREATE INDEX idx__node_is_token__:id
  ON facts_node_:id
  USING btree
  (is_token) WITH(FILLFACTOR=100);

CREATE INDEX idx__node_left__:id
  ON facts_node_:id
  USING btree
  ("left") WITH(FILLFACTOR=100);


CREATE INDEX idx__node_left_token__:id
  ON facts_node_:id
  USING btree
  (left_token) WITH(FILLFACTOR=100);

CREATE INDEX idx__node_seg_name__:id
  ON facts_node_:id
  USING btree
  (seg_name) WITH(FILLFACTOR=100);

CREATE INDEX idx__node_seg_index__:id
  ON facts_node_:id
  USING btree
  (seg_index) WITH(FILLFACTOR=100);

CREATE INDEX idx__node_name__:id
  ON facts_node_:id
  USING btree
  ("name" varchar_pattern_ops) WITH(FILLFACTOR=100);


CREATE INDEX idx__node_namespace__:id
  ON facts_node_:id
  USING btree
  (namespace varchar_pattern_ops) WITH(FILLFACTOR=100);

CREATE INDEX idx__node_right__:id
  ON facts_node_:id
  USING btree
  ("right") WITH(FILLFACTOR=100);


CREATE INDEX idx__node_right_token__:id
  ON facts_node_:id
  USING btree
  (right_token) WITH(FILLFACTOR=100);

CREATE INDEX idx__node_span__:id
  ON facts_node_:id
  USING btree
  (span varchar_pattern_ops) WITH(FILLFACTOR=100) WHERE span IS NOT NULL;


CREATE INDEX idx__node_token__:id
  ON facts_node_:id
  USING btree
  (token_index) WITH(FILLFACTOR=100);

CREATE INDEX idx__node_corpus_ref__:id
  ON facts_node_:id
  USING btree
  (corpus_ref) WITH(FILLFACTOR=100);

CREATE INDEX idx__node_text_ref__:id
  ON facts_node_:id
  USING btree
  (text_ref) WITH(FILLFACTOR=100);

CREATE INDEX idx__node_rownum__:id
  ON facts_node_:id
  USING btree
  (n_na_rownum) WITH(FILLFACTOR=100);

-- node_annotation
CREATE INDEX idx__nanno_val__:id
  ON facts_node_:id
  USING btree
  (val varchar_pattern_ops) WITH(FILLFACTOR=100);

CREATE INDEX idx__nanno_val_ns__:id
  ON facts_node_:id
  USING btree
  (val_ns varchar_pattern_ops) WITH(FILLFACTOR=100);


----- 2nd query
CREATE INDEX idx__2nd_query_:id ON facts_node_:id (corpus_ref, text_ref,left_token, right_token)
 WITH(FILLFACTOR=100);

END; -- transaction
