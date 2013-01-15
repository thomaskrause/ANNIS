UPDATE
  corpus_stats
SET 
  max_component_id = (SELECT max(id) + 1 FROM component_:id),
  max_node_id = (SELECT max(id) + 1  FROM node_:id)
WHERE
  id  = :id
;
