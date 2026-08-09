CREATE CONSTRAINT document_id_unique IF NOT EXISTS
FOR (node:Document)
REQUIRE node.documentId IS UNIQUE;

CREATE CONSTRAINT technology_name_unique IF NOT EXISTS
FOR (node:Technology)
REQUIRE node.name IS UNIQUE;

CREATE CONSTRAINT author_name_unique IF NOT EXISTS
FOR (node:Author)
REQUIRE node.name IS UNIQUE;

CREATE CONSTRAINT organization_name_unique IF NOT EXISTS
FOR (node:Organization)
REQUIRE node.name IS UNIQUE;

CREATE CONSTRAINT topic_name_unique IF NOT EXISTS
FOR (node:Topic)
REQUIRE node.name IS UNIQUE;