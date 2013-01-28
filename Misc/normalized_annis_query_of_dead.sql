﻿
-- no node duplication

-- "harmless"  cat="S" & /d.*/ & /(e|f|h).*/ & #1 >* #2 & #1 >* #3 & #2 .1,40 #3 query

SELECT 
  count(*)
FROM
  (
    SELECT DISTINCT
    rank1.node_ref AS id1,
    node2.id AS id2,
    node3.id AS id3,
    rank1.toplevel_corpus
  FROM
    --node_2181 AS node1,
    rank_2181 AS rank1,
    node_annotation_2181 AS node_annotation1,
    node_2181 AS node2,
    rank_2181 AS rank2,
    node_2181 AS node3, 
    rank_2181 AS rank3
  WHERE
    -- join conditions
    --node1.id = rank1.node_ref AND
    node_annotation1.node_ref = rank1.node_ref AND
    node2.id = rank2.node_ref AND
    node3.id = rank3.node_ref AND
    node_annotation1.corpus_ref = rank1.corpus_ref AND
    node2.corpus_ref = rank2.corpus_ref AND
    node3.corpus_ref = rank3.corpus_ref AND
    
    -- real restrictions
    rank1.corpus_ref = rank3.corpus_ref AND
    rank1.corpus_ref = rank2.corpus_ref AND

    rank1.pre < rank3.pre AND
    rank3.pre < rank1.post AND
    
    rank1.pre < rank2.pre AND
    rank2.pre < rank1.post AND    
    

    rank1.type_ref = 4 AND
    rank2.type_ref = 4 AND
    rank3.type_ref = 4 AND
    
    node_annotation1.val = 'cat:S' AND
    node2.right_token BETWEEN SYMMETRIC node3.left_token - 1 AND node3.left_token - 40 AND
    node2.span ~ '^d.*$' AND
    node2.text_ref = node3.text_ref AND
    --node3.span = 'Haus'
    node3.span ~ '^(e|f|h).*$'
  ) AS solutions

