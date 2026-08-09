MERGE (document:Document {
  documentId: 'graph-engineering-v2026.08.02.pdf'
});

MERGE (knowledgeTopic:Topic {
  name: '지식 노드'
});

MERGE (executionTopic:Topic {
  name: '실행 노드'
});

MATCH (document:Document {
  documentId: 'graph-engineering-v2026.08.02.pdf'
}),
      (knowledgeTopic:Topic {
        name: '지식 노드'
      }),
      (executionTopic:Topic {
        name: '실행 노드'
      })
MERGE (document)-[:HAS_TOPIC]->(knowledgeTopic)
MERGE (document)-[:HAS_TOPIC]->(executionTopic);