WITH info AS
(
  SELECT min(child.id) minid, min(child.pre) AS minpre
  FROM corpus AS top, corpus AS child
  WHERE
    top.id= :id AND
    top.pre <= child.pre AND child.post <= top.post
)
SELECT id - minid AS id, "name", type, version, pre - minpre AS pre, post - minpre AS post
FROM corpus, info
WHERE
  pre >= :id AND post <= :id
ORDER BY id
