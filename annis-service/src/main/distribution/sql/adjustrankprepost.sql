ALTER TABLE _rank RENAME pre TO id;
ALTER TABLE _rank ADD pre integer;

DROP TABLE IF EXISTS _premin;
CREATE UNLOGGED TABLE _premin (
  corpus_ref integer PRIMARY KEY,
  minpre integer
);

INSERT INTO _premin(corpus_ref, minpre)
SELECT corpus_ref, min(id) as minpre FROM _rank GROUP BY corpus_ref;

UPDATE _rank AS r SET 
  pre = id - (SELECT minpre FROM _premin AS m WHERE r.corpus_ref = m.corpus_ref),
  post = post - (SELECT minpre FROM _premin AS m WHERE r.corpus_ref = m.corpus_ref),
  parent = parent - (SELECT minpre FROM _premin AS m WHERE r.corpus_ref = m.corpus_ref)
;

ALTER TABLE _rank ALTER COLUMN pre SET NOT NULL;

DROP TABLE _premin;
