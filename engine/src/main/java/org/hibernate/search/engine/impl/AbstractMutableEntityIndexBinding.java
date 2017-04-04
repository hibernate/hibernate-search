/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Set;

import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public abstract class AbstractMutableEntityIndexBinding implements MutableEntityIndexBinding {

	private final Similarity similarityInstance;
	private DocumentBuilderIndexedEntity documentBuilder;
	private final IndexManagerType indexManagerType;
	private final EntityIndexingInterceptor<?> entityIndexingInterceptor;

	public AbstractMutableEntityIndexBinding(
			Similarity similarityInstance,
			IndexManagerType indexManagerType,
			EntityIndexingInterceptor<?> entityIndexingInterceptor) {
		this.similarityInstance = similarityInstance;
		this.indexManagerType = indexManagerType;
		this.entityIndexingInterceptor = entityIndexingInterceptor;
	}

	@Override
	public void setDocumentBuilderIndexedEntity(DocumentBuilderIndexedEntity documentBuilder) {
		this.documentBuilder = documentBuilder;
	}

	@Override
	public Similarity getSimilarity() {
		return similarityInstance;
	}

	@Override
	public DocumentBuilderIndexedEntity getDocumentBuilder() {
		return documentBuilder;
	}

	@Override
	public void postInitialize(Set<Class<?>> indexedClasses) {
		documentBuilder.postInitialize( indexedClasses );
	}

	@Override
	public IndexManagerType getIndexManagerType() {
		return indexManagerType;
	}

	@Override
	public EntityIndexingInterceptor<?> getEntityIndexingInterceptor() {
		return entityIndexingInterceptor;
	}

}
