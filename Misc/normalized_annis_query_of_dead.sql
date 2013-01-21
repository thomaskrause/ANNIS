-- no node duplication

-- "harmless"  cat="S" & /d.*/ & /(e|f|h).*/ & #1 >* #2 & #1 >* #3 & #2 .1,4 #3 query

--CREATE INDEX testidx__node_span__2379  ON node_2379  USING btree  (span varchar_pattern_ops, corpus_ref, id);
--analyze node_2379

--alter table rank_2379 ADD CONSTRAINT "prepost" CHECK(pre < post);

SELECT 
  count(*)
FROM
  (
    SELECT DISTINCT
    node1.id AS id1, node1.corpus_ref AS corpus1,
    node2.id AS id2, node2.corpus_ref AS corpus2,
    node3.id AS id3, node3.corpus_ref AS corpus3,
    node1.toplevel_corpus
  FROM
    node_2379 AS node1,
    node_2379 AS node2,
    node_2379 AS node3,
    component_2379 AS component1,
    component_2379 AS component2,
    component_2379 AS component3,
    rank_2379 AS rank1,
    rank_2379 AS rank2,
    rank_2379 AS rank3,
    node_annotation_2379 AS node_annotation1
  WHERE
    -- TODO: joins on source tables
    rank1.component_ref = component1.id AND
    rank1.corpus_ref = component1.corpus_ref AND
    rank2.component_ref = component2.id AND
    rank2.corpus_ref = component2.corpus_ref AND
    rank3.component_ref = component3.id AND
    rank3.corpus_ref = component3.corpus_ref AND
    rank1.node_ref = node1.id AND
    rank1.corpus_ref = node1.corpus_ref AND
    rank2.node_ref = node2.id AND
    rank2.corpus_ref = node2.corpus_ref AND
    rank3.node_ref = node3.id AND
    rank3.corpus_ref = node3.corpus_ref AND
    node_annotation1.node_ref = node1.id AND
    node_annotation1.corpus_ref = node1.corpus_ref AND
    
    -- real restrictions
    rank1.component_ref = rank3.component_ref AND rank1.corpus_ref = rank3.corpus_ref AND
    rank1.component_ref = rank2.component_ref AND rank1.corpus_ref = rank2.corpus_ref AND

    rank2.pre BETWEEN rank1.pre AND rank1.post AND
    rank3.pre BETWEEN rank1.pre AND rank1.post AND
    
    --rank1.pre < rank3.pre AND
    --rank1.pre < rank2.pre AND
    component2."name" IS NULL AND
    component2."type" = 'd' AND
    component3."name" IS NULL AND
    component3."type" = 'd' AND
    --rank3.pre < rank1.post AND
    --rank2.pre < rank1.post AND    
    node1.corpus_ref = node2.corpus_ref AND
    node1.corpus_ref = node3.corpus_ref AND
    node_annotation1.val = 'cat:S' AND
    node2.corpus_ref = node3.corpus_ref AND
    node2.right_token BETWEEN SYMMETRIC node3.left_token - 1 AND node3.left_token - 40 AND
    node2.span ~ '^d.*$' AND
    node2.text_ref = node3.text_ref AND
    --node3.span = 'Haus'
    node3.span ~ '^(e|f|h).*$'
  ) AS solutions