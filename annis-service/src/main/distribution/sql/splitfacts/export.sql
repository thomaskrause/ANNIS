DROP TABLE IF EXISTS _export_corpus;
CREATE TEMPORARY TABLE _export_corpus AS
(
  SELECT id - :minid AS id, "name", type, version, pre - :minpre AS pre, post - :minpre AS post
  FROM corpus
  WHERE
    pre >= :toppre AND post <= :toppost
  ORDER BY id
);

DROP TABLE IF EXISTS _export_corpus_annotation;
CREATE TEMPORARY TABLE _export_corpus_annotation
AS 
(
  SELECT anno.corpus_ref - :minid AS corpus_ref, anno."namespace", anno."name", anno."value"
  FROM corpus_annotation AS anno, corpus AS child
  WHERE
    child.pre >= :toppre AND child.post <= :toppost AND
    anno.corpus_ref = child.id
  ORDER BY corpus_ref
)
;

DROP TABLE IF EXISTS _export_text;
CREATE TEMPORARY TABLE _export_text
AS 
(

  SELECT corpus_ref - :minid AS corpus_ref, id AS id, "name" AS "name", "text" AS "text"
  FROM text AS t
  WHERE
    toplevel_corpus = :id
  ORDER BY corpus_ref, id
);

DROP TABLE IF EXISTS _export_node;
CREATE TEMPORARY TABLE _export_node
AS 
(
  SELECT id, text_ref, corpus_ref - :minid, "namespace", "name", "left", "right", token_index, left_token, right_token, seg_index, seg_name, span 
  FROM facts_node
  WHERE
    toplevel_corpus = :id AND n_na_rownum = 1
  ORDER BY corpus_ref, id
);

DROP TABLE IF EXISTS _export_component;
CREATE TEMPORARY TABLE _export_component
AS 
(
  SELECT DISTINCT f.component_id AS id, t."type", t.namespace, t."name" 
  FROM facts_edge AS f, component_type AS t
  WHERE
    f.toplevel_corpus = :id AND r_ea_rownum = 1 AND
    t.toplevel_corpus = :id AND t.id = f.type_ref
  ORDER BY id
);

DROP TABLE IF EXISTS _export_rank;
CREATE TEMPORARY TABLE _export_rank
AS 
(
  SELECT pre, post, node_ref, component_id, corpus_ref - :minid, parent, "level"
  FROM facts_edge
  WHERE
    toplevel_corpus = :id AND r_ea_rownum = 1
  ORDER BY component_id, pre
);

DROP TABLE IF EXISTS _export_node_annotation;
CREATE TEMPORARY TABLE _export_node_annotation
AS 
(
  SELECT corpus_ref - :minid, id, split_part(val_ns, ':', 1) AS namespace, 
    split_part(val_ns, ':', 2) AS "name", 
    regexp_replace(val, '^[^:]+:', '') AS "value"
  FROM facts_node
  WHERE
    toplevel_corpus = :id AND val_ns IS NOT NULL
  ORDER BY corpus_ref, id, val_ns
);

DROP TABLE IF EXISTS _export_edge_annotation;
CREATE TEMPORARY TABLE _export_edge_annotation
AS 
(
  SELECT component_id, pre, split_part(val_ns, ':', 1) AS namespace, 
    split_part(val_ns, ':', 2) AS "name", 
    regexp_replace(val, '^[^:]+:', '') AS "value"
  FROM facts_edge
  WHERE
    toplevel_corpus = :id AND val_ns IS NOT NULL
  ORDER BY component_id, pre, val_ns
);

DROP TABLE IF EXISTS _export_resolver_vis_map;
CREATE TEMPORARY TABLE _export_resolver_vis_map
AS 
(
  SELECT v.corpus, v.version, v.namespace, v.element, v.vis_type, v.display_name, v.visibility, v."order", v.mappings
  FROM resolver_vis_map AS v, corpus AS c
  WHERE
    c.top_level IS TRUE AND c."name" = v.corpus AND
    c.id = :id
  ORDER BY corpus, "order"
);

DROP TABLE IF EXISTS _export_media_files;
CREATE TEMPORARY TABLE _export_media_files
AS 
(
  SELECT m.file, m.corpus_ref - :minid, m.bytes, m.mime_type, m.title
  FROM media_files AS m, corpus AS child
  WHERE
    child.pre >= :toppre AND child.post <= :toppost AND
    m.corpus_ref = child.id
  ORDER BY corpus_ref
);