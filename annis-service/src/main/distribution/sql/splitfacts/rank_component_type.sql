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

ALTER TABLE _component ADD COLUMN type_ref integer REFERENCES _component_type(id);
ALTER TABLE _rank ADD COLUMN type_ref integer REFERENCES _component_type(id);

UPDATE _component AS c SET type_ref = (
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
