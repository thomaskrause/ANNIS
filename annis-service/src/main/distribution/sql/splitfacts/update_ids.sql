UPDATE _corpus
SET 
 id = id + COALESCE((SELECT max_corpus_id FROM corpus_stats WHERE id = :id),0),
 pre = pre + COALESCE((SELECT max_corpus_post FROM corpus_stats WHERE id = :id),0),
 post = post + COALESCE((SELECT max_corpus_post FROM corpus_stats WHERE id = :id),0);

UPDATE _corpus_annotation SET corpus_ref = corpus_ref + COALESCE((SELECT max_corpus_id FROM corpus_stats WHERE id = :id),0);

UPDATE _node
SET 
 corpus_ref = corpus_ref + COALESCE((SELECT max_corpus_id FROM corpus_stats WHERE id = :id),0);

UPDATE _rank
SET 
 corpus_ref = corpus_ref + COALESCE((SELECT max_corpus_id FROM corpus_stats WHERE id = :id),0);

UPDATE _text
SET 
 corpus_ref = corpus_ref + COALESCE((SELECT max_corpus_id FROM corpus_stats WHERE id = :id),0);
