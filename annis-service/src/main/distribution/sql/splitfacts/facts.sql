--- :id is replaced by code
DROP TABLE IF EXISTS node_:id;
DROP TABLE IF EXISTS component_:id;
DROP TABLE IF EXISTS rank_:id;
DROP TABLE IF EXISTS node_annotation_:id;
DROP TABLE IF EXISTS edge_annotation_:id;

-- add is_token column to _node
ALTER TABLE _node ADD COLUMN is_token boolean;
ALTER TABLE _node DROP COLUMN seg_right;
ALTER TABLE _node RENAME COLUMN seg_left TO seg_index;

UPDATE _node SET is_token = token_index IS NOT NULL;

ALTER TABLE _component ADD COLUMN toplevel_corpus integer;
UPDATE _component SET toplevel_corpus = :id;

ALTER TABLE _rank ADD COLUMN toplevel_corpus integer;
UPDATE _rank SET toplevel_corpus = :id;
ALTER TABLE _rank ALTER COLUMN toplevel_corpus SET NOT NULL;

COMMIT;

ALTER TABLE _node INHERIT node;
ALTER TABLE _component INHERIT component;
ALTER TABLE _rank INHERIT rank;

ALTER TABLE _node RENAME TO node_:id;
ALTER TABLE _component RENAME TO component_:id;
ALTER TABLE _rank RENAME TO rank_:id;


CREATE TABLE node_annotation_:id (
  CHECK(toplevel_corpus = :id)
) INHERITS(node_annotation);
INSERT INTO node_annotation_:id(node_ref, val_ns, val, toplevel_corpus)
SELECT 
  node_ref, 
  namespace || ':' || "name" || ':' || "value",
  "name" || ':' || "value",
  :id
FROM _node_annotation;
  

CREATE TABLE edge_annotation_:id (
  CHECK(toplevel_corpus = :id)
) INHERITS(edge_annotation);
INSERT INTO edge_annotation_:id(rank_ref, val_ns, val, toplevel_corpus)
SELECT 
  rank_ref, 
  namespace || ':' || "name" || ':' || "value",
  "name" || ':' || "value",
  :id
FROM _edge_annotation;
