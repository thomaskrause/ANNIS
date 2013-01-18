-- setup computation of level for dominance and pointing relations

ALTER TABLE _rank ADD level integer;

CREATE INDEX ON _rank (pre, component_ref, corpus_ref);

UPDATE _rank c SET "level"=
(
  WITH RECURSIVE levelcalc AS
  (
    SELECT pre, parent, component_ref , _rank.corpus_ref
    FROM _rank, _component
    WHERE 
      c.pre = _rank.pre AND c.component_ref = _rank.component_ref AND
      _rank.component_ref = _component.id AND
      _component.type IN ('d', 'p')   AND
      _component.corpus_ref = _rank.corpus_ref
    UNION ALL
    
    SELECT a.pre, a.parent, a.component_ref, a.corpus_ref 
    FROM _rank a, levelcalc l 
    WHERE l.parent = a.pre AND l.component_ref = a.component_ref AND l.corpus_ref = a.corpus_ref
  )
  SELECT count(*) - 1 as "level" FROM levelcalc
)
FROM _component
WHERE
  c.component_ref = _component.id AND
  c.corpus_ref = _component.corpus_ref AND
  _component.type IN ('d', 'p')
;