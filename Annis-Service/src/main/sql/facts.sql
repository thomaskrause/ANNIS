--- :id is replaced by code

DROP TABLE IF EXISTS facts_:id;

CREATE TABLE facts_:id
(
  -- temporary columns for calculating the sample_*
  node_rownum INTEGER,
  node_anno_rownum INTEGER
  -- check constraints
  CHECK(toplevel_corpus = :id)
)
INHERITS (facts);

INSERT INTO facts_:id
(
  id,
  text_ref,
	corpus_ref,
	toplevel_corpus,
  node_namespace,
	node_name,
	"left",
	"right",
	token_index,
  is_token,
	continuous,
	span,
	left_token,
	right_token,
  pre,
	post,
	parent,
	root,
	"level",
  component_id,
	edge_type,
	edge_namespace,
	edge_name,
	node_annotation_namespace,
	node_annotation_name,
	node_annotation_value,
  edge_annotation_namespace,
	edge_annotation_name,
	edge_annotation_value,
  sample_node,
  sample_node_annotation,
  node_rownum,
  node_anno_rownum
)

SELECT
*,
    row_number() OVER (PARTITION BY id) AS node_rownum,
    row_number() OVER (PARTITION BY id, node_annotation_namespace, node_annotation_name, node_annotation_value) AS node_anno_rownum
FROM
(
  SELECT
    _node.id AS id,
    _node.text_ref AS text_ref,
    _node.corpus_ref AS corpus_ref,
    _node.toplevel_corpus AS toplevel_corpus,
    _node.namespace AS node_namespace,
    _node.name AS node_name,
    _node."left" AS "left",
    _node."right" AS "right",
    _node.token_index AS token_index,
    FALSE AS is_token,
    _node.continuous AS continuous,
    _node.span AS span,
    _node.left_token AS left_token,
    _node.right_token AS right_token,

    _rank.pre AS pre,
    _rank.post AS post,
    _rank.parent AS parent,
    _rank.root AS root,
    _rank.level AS level,

    _component.id AS component_id,
    _component.type AS edge_type,
    _component.namespace AS edge_namespace,
    _component.name AS edge_name,

    _node_annotation.namespace AS node_annotation_namespace,
    _node_annotation.name AS node_annotation_name,
    _node_annotation.value AS node_annotation_value,

    _edge_annotation.namespace AS edge_annotation_namespace,
    _edge_annotation.name AS edge_annotation_name,
    _edge_annotation.value AS edge_annotation_value,
    false AS sample_node,
    false AS sample_node_anno
  FROM
    _node
    JOIN _rank ON (_rank.node_ref = _node.id)
    JOIN _component ON (_rank.component_ref = _component.id)
    LEFT JOIN _node_annotation ON (_node_annotation.node_ref = _node.id)
    LEFT JOIN _edge_annotation ON (_edge_annotation.rank_ref = _rank.pre)
  WHERE
    _node.toplevel_corpus = :id
) as tmp
;


-- can't be run inside transaction
-- VACUUM ANALYZE facts;