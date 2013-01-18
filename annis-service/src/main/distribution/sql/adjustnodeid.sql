ALTER TABLE _node_annotation ADD COLUMN corpus_ref integer;
UPDATE _node_annotation SET corpus_ref = (SELECT corpus_ref FROM _node WHERE node_ref = id);

ALTER TABLE _rank ADD COLUMN corpus_ref integer;
UPDATE _rank SET corpus_ref = (SELECT corpus_ref FROM _node WHERE node_ref = id);

DROP TABLE IF EXISTS _nodeid_min;
CREATE UNLOGGED TABLE _nodeid_min (
  corpus_ref integer PRIMARY KEY,
  min_id integer
);

INSERT INTO _nodeid_min(corpus_ref, min_id)
  SELECT corpus_ref, min(id) as min_id FROM _node GROUP BY corpus_ref;

UPDATE _node AS n SET 
  id = id - (SELECT min_id FROM _nodeid_min AS m WHERE n.corpus_ref = m.corpus_ref)
;

UPDATE _node_annotation AS na SET
  node_ref = node_ref - (SELECT min_id FROM _nodeid_min AS m WHERE na.corpus_ref = m.corpus_ref)
;

UPDATE _rank AS r SET
  node_ref = node_ref - (SELECT min_id FROM _nodeid_min AS m WHERE r.corpus_ref = m.corpus_ref)
;

DROP TABLE _nodeid_min;
