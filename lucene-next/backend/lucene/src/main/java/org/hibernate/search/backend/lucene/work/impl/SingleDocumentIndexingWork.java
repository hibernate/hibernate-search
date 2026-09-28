package org.hibernate.search.backend.lucene.work.impl;


public interface SingleDocumentIndexingWork extends IndexingWork<Long> {

	String getEntityTypeName();

	Object getEntityIdentifier();

}
