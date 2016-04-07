WITH corpusselection AS (
  SELECT sub.* FROM corpus AS sub, corpus AS top 
  WHERE top.id=:id AND top.top_level IS TRUE AND sub.pre >= top.pre AND sub.post <= top.post
)
UPDATE corpus_stats SET 
  text =  (SELECT count(*) FROM text WHERE toplevel_corpus = :id),
  tokens = (SELECT count(distinct id) FROM facts WHERE toplevel_corpus = :id AND is_token IS TRUE),
  max_corpus_id = (SELECT max(id) FROM corpusselection),
  max_corpus_pre = (SELECT max(pre) FROM corpusselection),
  max_corpus_post = (SELECT max(post) FROM corpusselection) ,
  max_node_id = (SELECT max(id) FROM facts WHERE toplevel_corpus = :id)
WHERE id = :id