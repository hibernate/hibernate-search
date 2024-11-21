/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Document;

public class LuceneIndexEntry implements Iterable<Document> {

	private final String indexName;

	private final String id;

	private final List<Document> documents;

	LuceneIndexEntry(String indexName, String id, List<Document> documents) {
		this.indexName = indexName;
		this.id = id;
		this.documents = documents;
	}

	@Override
	public Iterator<Document> iterator() {
		return documents.iterator();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexName=" ).append( indexName )
				.append( ", id=" ).append( id )
				.append( ", associatedDocuments=" ).append( documents )
				.append( "]" );
		return sb.toString();
	}
}
