--- :id is replaced by code
DROP TABLE IF EXISTS node_:id CASCADE;
DROP TABLE IF EXISTS component_type_:id CASCADE;
DROP TABLE IF EXISTS rank_:id CASCADE;
DROP TABLE IF EXISTS node_annotation_:id CASCADE;
DROP TABLE IF EXISTS edge_annotation_:id CASCADE;
DROP TABLE IF EXISTS text_:id CASCADE;

CREATE UNLOGGED TABLE text_:id (
  id    integer PRIMARY KEY,
  toplevel_corpus integer REFERENCES corpus(id),
  CHECK(toplevel_corpus = :id)
) INHERITS(text);

INSERT INTO text_:id (id, "name", text, toplevel_corpus)
SELECT id, "name", text, :id FROM _text;

CREATE UNLOGGED TABLE node_:id (
  text_ref integer REFERENCES text_:id(id),
  corpus_ref integer REFERENCES corpus(id),
  toplevel_corpus integer REFERENCES corpus(id),
  PRIMARY KEY(id),
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
CREATE UNLOGGED TABLE node_annotation_:id (
  toplevel_corpus integer REFERENCES corpus(id),
  PRIMARY KEY(node_ref, val_ns),
  FOREIGN KEY (node_ref) REFERENCES node_:id(id),
  CHECK(toplevel_corpus = :id)
) INHERITS(node_annotation);

INSERT INTO node_annotation_:id(node_ref, val_ns, val, toplevel_corpus)
SELECT 
  node_ref, 
  namespace || ':' || "name" || ':' || "value",
  "name" || ':' || "value",
  :id
FROM _node_annotation;

--
CREATE TABLE component_type_:id
(
  id integer PRIMARY KEY,
  toplevel_corpus integer REFERENCES corpus(id),
  UNIQUE("type", namespace, "name"),
  CHECK(toplevel_corpus = :id)
) INHERITS(component_type);

INSERT INTO component_type_:id(id, "type", namespace, "name", toplevel_corpus)
SELECT id, "type", namespace, "name", :id
FROM _component_type;

--
CREATE UNLOGGED TABLE rank_:id (
  id integer PRIMARY KEY,
  component_ref integer,
  type_ref integer REFERENCES component_type_:id (id),
  toplevel_corpus integer REFERENCES corpus(id),

  CHECK(toplevel_corpus = :id),

  UNIQUE (component_ref, pre),
  UNIQUE (component_ref, post),
  UNIQUE (component_ref, pre, post),
  FOREIGN KEY (node_ref) REFERENCES node_:id(id)
) INHERITS(rank);

INSERT INTO rank_:id (node_ref, id, pre, post, parent, root, "level", 
  component_ref, type_ref, toplevel_corpus)
SELECT node_ref, id, pre, post, parent, root, "level", component_ref, type_ref, :id
FROM _rank;

--
CREATE UNLOGGED TABLE edge_annotation_:id (
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
