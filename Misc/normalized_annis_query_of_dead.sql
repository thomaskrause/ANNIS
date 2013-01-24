
-- no node duplication

-- "harmless"  cat="S" & /d.*/ & /(e|f|h).*/ & #1 >* #2 & #1 >* #3 & #2 .1,40 #3 query

--CREATE INDEX new__rank_typeref__2181 ON rank_2181(type_ref);
--CREATE INDEX new__rank_test1__2181 ON rank_2181(type_ref, corpus_ref, node_ref);

-- CREATE INDEX new__node_annotation_val__2181 ON node_annotation_2181 (val  varchar_pattern_ops, corpus_ref, node_ref);
--CREATE INDEX new__rank_prepost__2181 ON rank_2181(corpus_ref, component_ref, pre, post);

--CREATE INDEX new__node_span_corpus_id__2181 ON node_2181(span varchar_pattern_ops, corpus_ref, id);
--CREATE INDEX new__rank_type_corpus_node__2181 ON rank_2181("type_ref", corpus_ref, node_ref);

--CREATE INDEX new__rank_type4_corpus_node__2181 ON rank_2181(corpus_ref, node_ref) WHERE type_ref = 4;

--CREATE INDEX idx__cluster__2181 ON rank_2181(type_ref, corpus_ref, component_ref, node_ref);
--CLUSTER rank_2181 USING idx__cluster__2181;
--DROP INDEX idx__cluster__2181;

--REINDEX TABLE rank_2181;
--analyze rank_2181;

--set enable_mergejoin=true;
--set enable_hashjoin=true;
--analyze node_annotation_2181

--analyze edge_annotation_2181;

--alter TABLE rank_2181 ALTER COLUMN pre SET(n_distinct=592);
--alter TABLE rank_2181 ALTER COLUMN post SET(n_distinct=612);
--alter TABLE rank_2181 ALTER COLUMN component_ref SET(n_distinct=1094);
--alter TABLE rank_2181 ALTER COLUMN node_ref SET(n_distinct=10673);

--ALTER TABLE rank_2181
--  ADD CONSTRAINT rank_2181_corpus_ref_component_ref_pre_type_ref_key UNIQUE(corpus_ref, component_ref, pre, type_ref);

alter table rank_2181 alter column node_ref set statistics 10;
alter table rank_2181 alter column corpus_ref set statistics 10;

alter table node_2181 alter column id set statistics 5000;
alter table node_2181 alter column corpus_ref set statistics 5000;

analyze rank_2181
analyze node_2181

--reindex table node_annotation_2181

--set constraint_exclusion=on;

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
    node_2181 AS node1 
      JOIN rank_2181 AS rank1 ON (node1.id = rank1.node_ref AND node1.corpus_ref = rank1.corpus_ref)
      JOIN node_annotation_2181 AS node_annotation1 ON (node_annotation1.node_ref = node1.id AND node_annotation1.corpus_ref = node1.corpus_ref),
    node_2181 AS node2 
      JOIN rank_2181 AS rank2 ON (node2.id = rank2.node_ref AND node2.corpus_ref = rank2.corpus_ref),
    node_2181 AS node3 
      JOIN rank_2181 AS rank3 ON (node3.id = rank3.node_ref AND node3.corpus_ref = rank3.corpus_ref)
  WHERE
    
    -- real restrictions
    rank1.component_ref = rank3.component_ref AND rank1.corpus_ref = rank3.corpus_ref AND rank1.type_ref = rank3.type_ref AND
    rank1.component_ref = rank2.component_ref AND rank1.corpus_ref = rank2.corpus_ref AND rank1.type_ref = rank2.type_ref AND

    rank1.pre < rank3.pre AND
    rank1.pre < rank2.pre AND

    rank1.type_ref = 4 AND
    rank2.type_ref = 4 AND
    rank3.type_ref = 4 AND
    
    rank3.pre < rank1.post AND
    rank2.pre < rank1.post AND    
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

select distinct corpus_ref from rank_2181

select min(node_ref), max(node_ref) from rank_2181

select count(*) FROm
(
WITH selected AS
(
select id, corpus_ref 
from node_2181
order by random() limit 10000
)
select distinct *  from rank_2181 as r, selected AS n
WHERE
  r.corpus_ref = n.corpus_ref AND
  r.node_ref = n.id
) as c
