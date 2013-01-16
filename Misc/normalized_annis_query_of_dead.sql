-- no node duplication

-- "harmless"  cat="S" & /d.*/ & /(e|f|h).*/ & #1 >* #2 & #1 >* #3 & #2 .1,4 #3 query
SELECT 
  count(*)
FROM
  (
    SELECT DISTINCT
    node1.id AS id1, node2.id AS id2, node3.id AS id3, node1.toplevel_corpus
  FROM
    node_1978 AS node1,
    node_1978 AS node2,
    node_1978 AS node3,
    component_1978 AS component1,
    component_1978 AS component2,
    component_1978 AS component3,
    rank_1978 AS rank1,
    rank_1978 AS rank2,
    rank_1978 AS rank3,
    node_annotation_1978 AS node_annotation1
  WHERE
    -- TODO: joins on source tables
    rank1.component_ref = component1.id AND
    rank2.component_ref = component2.id AND
    rank3.component_ref = component3.id AND
    rank1.node_ref = node1.id AND
    rank2.node_ref = node2.id AND
    rank3.node_ref = node3.id AND
    node_annotation1.node_ref = node1.id AND
    -- real restrictions
    component1.id = component3.id AND
    component1.id = component2.id AND
    rank1.pre < rank3.pre AND
    rank1.pre < rank2.pre AND
    component2."name" IS NULL AND
    component2."type" = 'd' AND
    component3."name" IS NULL AND
    component3."type" = 'd' AND
    rank3.pre < rank1.post AND
    rank2.pre < rank1.post AND    
    node1.corpus_ref = node2.corpus_ref AND
    node1.corpus_ref = node3.corpus_ref AND
    node_annotation1.val = 'cat:S' AND
    node2.corpus_ref = node3.corpus_ref AND
    node2.right_token BETWEEN SYMMETRIC node3.left_token - 1 AND node3.left_token - 40 AND
    node2.span ~ '^d.*$' AND
    node2.text_ref = node3.text_ref AND
    node3.span ~ '^(e|f|h).*$'
  ) AS solutions