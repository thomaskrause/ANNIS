ALTER TABLE _component ADD COLUMN corpus_ref integer;
UPDATE _component AS c SET corpus_ref = (SELECT DISTINCT corpus_ref FROM _rank WHERE component_ref = c.id);


DROP TABLE IF EXISTS _componentid_min;
CREATE UNLOGGED TABLE _componentid_min (
  corpus_ref integer PRIMARY KEY,
  min_id integer
);

INSERT INTO _componentid_min(corpus_ref, min_id)
  SELECT corpus_ref, min(component_ref) as min_id FROM _rank GROUP BY corpus_ref;

UPDATE _component AS c SET 
  id = id - (SELECT min_id FROM _componentid_min AS m WHERE c.corpus_ref = m.corpus_ref)
;

UPDATE _rank AS r SET
  component_ref = component_ref - (SELECT min_id FROM _componentid_min AS m WHERE r.corpus_ref = m.corpus_ref)
;

DROP TABLE _componentid_min;
