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
)
;
