--- :id is replaced by code
DROP TABLE IF EXISTS node_:id;
DROP TABLE IF EXISTS component_:id;
DROP TABLE IF EXISTS rank_:id;
DROP TABLE IF EXISTS node_annotation_:id;
DROP TABLE IF EXISTS edge_annotation_:id;

CREATE TABLE node_:id (
  corpus_ref integer REFERENCES corpus(id),
  toplevel_corpus integer REFERENCES corpus(id),
  PRIMARY KEY(corpus_ref, id),
  CHECK(toplevel_corpus = :id)
) INHERITS(node);

INSERT INTO node_:id(
  id, text_ref, corpus_ref, toplevel_corpus, "namespace", "name", 
  "left", "right", token_index, is_token, continuous, span, 
  left_token, right_token, seg_name, seg_index)
SELECT
  id, text_ref, corpus_ref, :id, "namespace", "name", 
  "left", "right", token_index, token_index IS NOT NULL, continuous, span,
  left_token, right_token, seg_name, seg_left
FROM _node;

--
CREATE TABLE node_annotation_:id (
  corpus_ref integer REFERENCES corpus(id),
  toplevel_corpus integer REFERENCES corpus(id),
  PRIMARY KEY(corpus_ref, node_ref, val_ns),
  FOREIGN KEY (corpus_ref, node_ref) REFERENCES node_:id(corpus_ref, id),
  CHECK(toplevel_corpus = :id)
) INHERITS(node_annotation);

INSERT INTO node_annotation_:id(corpus_ref, node_ref, val_ns, val, toplevel_corpus)
SELECT 
  corpus_ref,
  node_ref, 
  namespace || ':' || "name" || ':' || "value",
  "name" || ':' || "value",
  :id
FROM _node_annotation;

--
CREATE TABLE component_:id (
  id integer PRIMARY KEY,
  toplevel_corpus integer REFERENCES corpus(id),
  CHECK(toplevel_corpus = :id)
) INHERITS(component);

INSERT INTO component_:id (id, "type", "namespace", "name", toplevel_corpus)
SELECT id, "type", "namespace", "name", :id
FROM _component;

--
CREATE TABLE rank_:id (
  corpus_ref integer REFERENCES corpus(id),
  id integer PRIMARY KEY,
  component_ref integer REFERENCES component_:id(id),
  toplevel_corpus integer REFERENCES corpus(id),
  CHECK(toplevel_corpus = :id),
  UNIQUE (component_ref, pre, toplevel_corpus),
  FOREIGN KEY (corpus_ref, node_ref) REFERENCES node_:id(corpus_ref, id)
) INHERITS(rank);

INSERT INTO rank_:id (corpus_ref, node_ref, id, pre, post, parent, root, "level", 
  component_ref, toplevel_corpus)
SELECT corpus_ref, node_ref, id, pre, post, parent, root, "level", component_ref, :id
FROM _rank;
  
--
CREATE TABLE edge_annotation_:id (
  rank_ref integer REFERENCES rank_:id(id),
  toplevel_corpus integer REFERENCES corpus(id),
  PRIMARY KEY (rank_ref, val_ns),
  CHECK(toplevel_corpus = :id)
) INHERITS(edge_annotation);

INSERT INTO edge_annotation_:id(rank_ref, val_ns, val, toplevel_corpus)
SELECT 
  rank_ref, 
  namespace || ':' || "name" || ':' || "value",
  "name" || ':' || "value",
  :id
FROM _edge_annotation;
