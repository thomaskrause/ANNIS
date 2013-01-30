CREATE OR REPLACE FUNCTION getComponentType("type" char, namespace varchar, 
  "name" varchar, toplevel_corpus integer[]) 
RETURNS smallint AS $f$
SELECT id 
FROM component_type 
WHERE 
  "type" = $1 AND
  namespace IS NOT DISTINCT FROM $2 AND
  name IS NOT DISTINCT FROM $3 AND
  toplevel_corpus = ANY($4)
LIMIT 1
;
$f$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION getComponentTypeNameOnly("type" char, 
  "name" varchar, toplevel_corpus integer[]) 
RETURNS smallint AS $f$
SELECT id 
FROM component_type 
WHERE 
  "type" = $1 AND
  name IS NOT DISTINCT FROM $2 AND
  toplevel_corpus = ANY($3)
LIMIT 1
;
$f$ LANGUAGE SQL IMMUTABLE;

