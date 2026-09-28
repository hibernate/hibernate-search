package org.hibernate.search.backend.elasticsearch.work.impl;

public interface SingleDocumentIndexingWork extends IndexingWork<Void> {

	String getEntityTypeName();

	Object getEntityIdentifier();

}
