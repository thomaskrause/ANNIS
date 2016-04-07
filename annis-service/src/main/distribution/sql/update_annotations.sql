DELETE FROM annotations_:id;

INSERT INTO annotations_:id
(
  toplevel_corpus,
	namespace,
  name,
  value,
  occurences,
  "type",
  subtype,
  edge_namespace,
  edge_name
)
SELECT DISTINCT :id, (splitanno(node_qannotext))[1], (splitanno(node_qannotext))[2], 
  (splitanno(node_qannotext))[3], count(*) as occurences, 
  'node', 'n', NULL, NULL
FROM facts_:id
WHERE
node_qannotext is not null AND n_na_sample IS TRUE
GROUP BY (splitanno(node_qannotext))[1], (splitanno(node_qannotext))[2], (splitanno(node_qannotext))[3]

UNION

SELECT DISTINCT :id, (splitanno(edge_qannotext))[1], (splitanno(edge_qannotext))[2], 
  (splitanno(edge_qannotext))[3], count(distinct rank_id) as occurences,
  'edge', edge_type, edge_namespace, edge_name
FROM facts_:id
WHERE
  (edge_name IS NOT NULL) OR (edge_type = 'd')
GROUP BY (splitanno(edge_qannotext))[1], (splitanno(edge_qannotext))[2], (splitanno(edge_qannotext))[3], edge_type, edge_namespace, edge_name

UNION

SELECT DISTINCT :id, NULL as node_namespace, seg_name, NULL AS VALUE, count(*) AS occurences,
  'segmentation', NULL AS sub_type, NULL AS edge_namespace, NULL AS edge_name
FROM facts_:id 
WHERE seg_name IS NOT NULL and n_sample IS TRUE
GROUP BY(seg_name)
;