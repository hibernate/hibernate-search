package org.hibernate.search.util.impl.integrationtest.mapper.stub;

public interface SingleFieldDocumentBuilder<T> {

	void emptyDocument(String documentId);

	void document(String documentId, T fieldValue);

}
