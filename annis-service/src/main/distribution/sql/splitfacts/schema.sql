-- (modified) source tables

CREATE TABLE repository_metadata
(
  name varchar NOT NULL PRIMARY KEY,
  "value" varchar NOT NULL
);


CREATE TABLE corpus
(
  id         integer PRIMARY KEY,
  name       varchar NOT NULL, -- UNIQUE,
  type       varchar NOT NULL,
  version    varchar,
  pre        integer NOT NULL UNIQUE,
  post       integer NOT NULL UNIQUE,
  top_level  boolean NOT NULL,  -- true for roots of the corpus forest
  path_name  varchar[]
);
COMMENT ON COLUMN corpus.id IS 'primary key';
COMMENT ON COLUMN corpus.name IS 'name of the corpus';
COMMENT ON COLUMN corpus.pre IS 'pre-order value';
COMMENT ON COLUMN corpus.post IS 'post-order value';
COMMENT ON COLUMN corpus.path_name IS 'path of this corpus in the corpus tree (names)';

CREATE TABLE corpus_annotation
(
  corpus_ref  integer NOT NULL REFERENCES corpus (id) ON DELETE CASCADE,
  namespace   varchar,
  name        varchar NOT NULL,
  value       varchar,
  UNIQUE (corpus_ref, namespace, name)
);
COMMENT ON COLUMN corpus_annotation.corpus_ref IS 'foreign key to corpus.id';
COMMENT ON COLUMN corpus_annotation.namespace IS 'optional namespace of annotation key';
COMMENT ON COLUMN corpus_annotation.name IS 'annotation key';
COMMENT ON COLUMN corpus_annotation.value IS 'annotation value';

CREATE TABLE text
(
  corpus_ref integer REFERENCES corpus(id),
  id    integer,
  name  varchar,
  text  text,
  toplevel_corpus integer REFERENCES corpus(id),
  PRIMARY KEY(corpus_ref, id)
);

COMMENT ON COLUMN text.id IS 'primary key';
COMMENT ON COLUMN text.name IS 'informational name of the primary data text';
COMMENT ON COLUMN text.text IS 'raw text data';

CREATE TABLE node (
  id integer PRIMARY KEY,
  text_ref integer,
  corpus_ref integer REFERENCES corpus(id),
  toplevel_corpus integer REFERENCES corpus(id),
  "namespace" varchar,
  "name" varchar,
  "left" integer,
  "right" integer,
  token_index integer,
  is_token boolean,
  continuous boolean,
  span varchar,
  left_token integer,
  right_token integer,
  seg_name varchar,
  seg_index integer,
  FOREIGN KEY (corpus_ref, text_ref) REFERENCES text(corpus_ref, id)
);

CREATE TABLE node_annotation
(
  node_ref 				integer,
	val_ns          varchar, 
	val             varchar,
  toplevel_corpus integer REFERENCES corpus(id),
  PRIMARY KEY (node_ref, val_ns),
  FOREIGN KEY (node_ref) REFERENCES node(id)
);

CREATE TABLE component_type
(
  id smallint PRIMARY KEY,
  "type" char(1),
  namespace varchar,
  "name" varchar,
  toplevel_corpus integer REFERENCES corpus(id),
  UNIQUE("type", namespace, "name")
);

CREATE TABLE rank (
  corpus_ref integer REFERENCES corpus(id),
  node_ref integer, -- node reference
  id integer PRIMARY KEY,
  pre integer NOT NULL, -- pre-order value
  post integer NOT NULL, -- post-order value
  parent integer, -- foreign key to rank.pre of the parent node, or NULL for roots
  root boolean,
  "level" smallint,
  type_ref smallint REFERENCES component_type(id),
  toplevel_corpus integer REFERENCES corpus(id),
  UNIQUE (component_ref, pre, toplevel_corpus),
  FOREIGN KEY (node_ref) REFERENCES node(id)
);

CREATE TABLE edge_annotation
(
	rank_ref 				integer	REFERENCES rank(id),
	val_ns          varchar, 
	val             varchar,
  toplevel_corpus integer REFERENCES corpus(id),
  PRIMARY KEY (rank_ref, val_ns)
);

CREATE TABLE media_files
(
  file  bytea NOT NULL,
  corpus_ref  integer NOT NULL REFERENCES corpus(id) ON DELETE CASCADE,
  bytes bigint NOT NULL,
  mime_type varchar NOT NULL,
  title varchar NOT NULL,
  UNIQUE (corpus_ref, title)
);


-- stats
CREATE TABLE corpus_stats
(
  name        varchar,
  id          integer NOT NULL REFERENCES corpus ON DELETE CASCADE,
  text        integer,
  tokens        bigint,
  max_corpus_id integer  NULL,
  max_corpus_pre integer NULL,
  max_corpus_post integer NULL,
  source_path varchar -- original path to the folder containing the relANNIS sources
);


CREATE VIEW corpus_info AS SELECT 
  name,
  id, 
  text,
  tokens,
  source_path
FROM 
  corpus_stats;
  
  
CREATE TYPE resolver_visibility AS ENUM (
  'permanent', 
  'visible',
  'hidden',
  'removed',
  'preloaded'
);

CREATE TABLE resolver_vis_map
(
  "id"   serial PRIMARY KEY,
  "corpus"   varchar,
  "version"   varchar,
  "namespace"  varchar,
  "element"    varchar CHECK (element = 'node' OR element = 'edge'),
  "vis_type"   varchar NOT NULL,
  "display_name"   varchar NOT NULL,
  "visibility"    resolver_visibility NOT NULL DEFAULT 'hidden',
  "order" integer default '0',
  "mappings" varchar,
   UNIQUE (corpus,version,namespace,element,vis_type)              
);
COMMENT ON COLUMN resolver_vis_map.id IS 'primary key';
COMMENT ON COLUMN resolver_vis_map.corpus IS 'the name of the supercorpus, part of foreign key to corpus.name,corpus.version';
COMMENT ON COLUMN resolver_vis_map.version IS 'the version of the corpus, part of foreign key to corpus.name,corpus.version';
COMMENT ON COLUMN resolver_vis_map.namespace IS 'the several layers of the corpus';
COMMENT ON COLUMN resolver_vis_map.element IS 'the type of the entry: node | edge';
COMMENT ON COLUMN resolver_vis_map.vis_type IS 'the abstract type of visualization: tree, discourse, grid, ...';
COMMENT ON COLUMN resolver_vis_map.display_name IS 'the name of the layer which shall be shown for display';
COMMENT ON COLUMN resolver_vis_map.visibility IS 'defines the visibility state of a corpus: permanent: is always shown and can not be toggled, visible: is shown and can be toggled, hidden: is not shown can be toggled';
COMMENT ON COLUMN resolver_vis_map.order IS 'the order of the layers, in which they shall be shown';
COMMENT ON COLUMN resolver_vis_map.mappings IS 'which annotations in this corpus correspond to fields expected by the visualization, e.g. the tree visualizer expects a node label, which is called "cat" by default but may be changed using this field';

CREATE TABLE annotations
(
  id bigserial NOT NULL,
  namespace varchar,
  "name" varchar,
  "value" varchar,
  occurences bigint,
  "type" varchar,
  "subtype" char(1),
  edge_namespace varchar,
  edge_name varchar,
  toplevel_corpus integer NOT NULL REFERENCES corpus (id) ON DELETE CASCADE,
  PRIMARY KEY (id)
);

CREATE TABLE user_config
(
  id varchar NOT NULL,
  config json,
  PRIMARY KEY(id)
);