DROP TABLE IF EXISTS _export_corpus;
CREATE TABLE _export_corpus AS
(
  WITH info AS
  (
    SELECT min(child.id) minid, min(child.pre) AS minpre
    FROM corpus AS top, corpus AS child
    WHERE
      top.id= :id AND
      top.pre <= child.pre AND child.post <= top.post
  )
  SELECT c.id - minid AS id, c."name", c.type, c.version, c.pre - minpre AS pre, c.post - minpre AS post
  FROM corpus AS c, corpus AS top, info
  WHERE
    c.pre >= top.pre AND c.post <= top.post AND
    top.id = :id
  ORDER BY id
)
;

DROP TABLE IF EXISTS _export_corpus_annotation;
CREATE TABLE _export_corpus_annotation
AS 
(
  WITH info AS
  (
    SELECT min(child.id) minid
    FROM corpus AS top, corpus AS child
    WHERE
      top.id= :id AND
      top.pre <= child.pre AND child.post <= top.post
  )
  SELECT anno.corpus_ref - minid AS corpus_ref, anno."namespace", anno."name", anno."value"
  FROM corpus_annotation AS anno, corpus AS child, corpus AS top, info
  WHERE
    child.pre >= top.pre AND child.post <= top.post AND
    top.id = :id AND
    anno.corpus_ref = child.id
  ORDER BY corpus_ref
)
;
