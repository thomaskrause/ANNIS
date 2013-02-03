--- :id is replaced by code
DROP TABLE IF EXISTS facts_node_:id CASCADE;
DROP TABLE IF EXISTS component_type_:id CASCADE;
DROP TABLE IF EXISTS facts_edge_:id CASCADE;
DROP TABLE IF EXISTS text_:id CASCADE;

CREATE UNLOGGED TABLE text_:id (
  corpus_ref integer REFERENCES corpus(id),
  toplevel_corpus integer REFERENCES corpus(id),
  PRIMARY KEY (corpus_ref, id),
  CHECK(toplevel_corpus = :id)
) INHERITS(text);

INSERT INTO text_:id (corpus_ref, id, "name", text, toplevel_corpus)
SELECT corpus_ref, id, "name", text, :id FROM _text;

CREATE UNLOGGED TABLE facts_node_:id (
  corpus_ref integer REFERENCES corpus(id),
  toplevel_corpus integer REFERENCES corpus(id),
  PRIMARY KEY(corpus_ref, id, n_na_rownum),
  FOREIGN KEY (corpus_ref, text_ref) REFERENCES text_:id (corpus_ref, id),
  CHECK(toplevel_corpus = :id)
) INHERITS(facts_node);

INSERT INTO facts_node_:id(
  id, text_ref, corpus_ref, toplevel_corpus, "namespace", "name", 
  "left", "right", token_index, is_token, continuous, span, 
  left_token, right_token, seg_name, seg_index, val, val_ns, n_na_rownum)
SELECT
  n.id, n.text_ref, n.corpus_ref, :id, n."namespace", n."name", 
  n."left", n."right", n.token_index, n.token_index IS NOT NULL, n.continuous, n.span,
  n.left_token, n.right_token, n.seg_name, n.seg_left,
  na."name" || ':' || na."value", na.namespace || ':' || na."name" || ':' || na."value", 
  (row_number() OVER (PARTITION BY n.id, n.corpus_ref))
FROM _node AS n LEFT JOIN _node_annotation AS na ON (n.id = na.node_ref AND n.corpus_ref = na.corpus_ref);

--
CREATE TABLE component_type_:id
(
  id smallint PRIMARY KEY,
  toplevel_corpus integer REFERENCES corpus(id),
  UNIQUE("type", namespace, "name"),
  CHECK(toplevel_corpus = :id)
) INHERITS(component_type);

INSERT INTO component_type_:id(id, "type", namespace, "name", toplevel_corpus)
SELECT id, "type", namespace, "name", :id
FROM _component_type;

--
CREATE UNLOGGED TABLE facts_edge_:id (
  corpus_ref integer REFERENCES corpus(id),
  type_ref smallint REFERENCES component_type_:id (id),
  toplevel_corpus integer REFERENCES corpus(id),
  PRIMARY KEY(corpus_ref, pre, r_ea_rownum),

  CHECK(toplevel_corpus = :id)

) INHERITS(facts_edge);

INSERT INTO facts_edge_:id (corpus_ref, node_ref, component_id, pre, post, parent, root, "level", 
  type_ref, toplevel_corpus, val, val_ns, r_ea_rownum)
SELECT r.corpus_ref, r.node_ref, r.component_ref, r.pre, r.post, r.parent, r.root, r."level", 
       r.type_ref, :id, 
        ea."name" || ':' || ea."value", ea.namespace || ':' || ea."name" || ':' || ea."value", 
       (row_number() OVER (PARTITION BY r.id))
FROM _rank AS r LEFT JOIN _edge_annotation AS ea ON (ea.rank_ref = r.id);
