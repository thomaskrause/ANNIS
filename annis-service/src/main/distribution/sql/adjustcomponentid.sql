DROP TABLE IF EXISTS _component_type CASCADE;
CREATE UNLOGGED TABLE _component_type
(
  id integer PRIMARY KEY,
  "type" char(1),
  namespace varchar,
  "name" varchar,
  UNIQUE("type", namespace, "name")
);

INSERT INTO _component_type(id, "type", namespace, "name")
  SELECT row_number() OVER () AS id, c.*
  FROM
  (
    SELECT DISTINCT "type", namespace, "name" FROM _component
  ) AS c;

ALTER TABLE _component ADD COLUMN corpus_ref integer;
ALTER TABLE _component ADD COLUMN type_ref integer REFERENCES _component_type(id);
ALTER TABLE _rank ADD COLUMN type_ref integer REFERENCES _component_type(id);

UPDATE _component AS c SET corpus_ref = (SELECT DISTINCT corpus_ref FROM _rank WHERE component_ref = c.id),
  type_ref = (
    SELECT id 
    FROM _component_type AS t 
    WHERE 
      c."type" IS NOT DISTINCT FROM t."type" AND 
      c."namespace" IS NOT DISTINCT FROM t."namespace" AND
      c."name" IS NOT DISTINCT FROM t."name"
 );

UPDATE _rank AS r SET
  type_ref = (
    SELECT type_ref 
    FROM _component AS c 
    WHERE 
      r.component_ref = c.id
);


DROP TABLE IF EXISTS _componentid_mapping;
CREATE UNLOGGED TABLE _componentid_mapping (
  old_id integer PRIMARY KEY,
  new_id integer
);

INSERT INTO _componentid_mapping(old_id, new_id)
  SELECT DISTINCT id AS old_id, id - min(id) OVER (PARTITION BY corpus_ref, type_ref) AS new_id
  FROM _component 
;

UPDATE _component AS c SET 
  id = (SELECT new_id FROM _componentid_mapping AS m WHERE c.id = m.old_id)
;

UPDATE _rank AS r SET
  component_ref = (SELECT new_id FROM _componentid_mapping AS m WHERE r.component_ref = m.old_id)
;

DROP TABLE _componentid_mapping;

