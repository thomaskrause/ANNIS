-- compute real roots
-- actually, roots of components that are not actual roots should link parent to their parent node (even though it is in another component)

ALTER TABLE _rank ADD root boolean;

CREATE INDEX _rank_corpus_parent_idx ON _rank (corpus_ref, node_ref, parent);
COMMIT;
ANALYZE _rank;

UPDATE _rank top SET root=
(SELECT count(distinct _rank.parent) = 0 FROM _rank WHERE _rank.node_ref = top.node_ref AND _rank.corpus_ref = top.corpus_ref);

-- rank was changed, reanalyze it
ANALYZE _rank(root);
