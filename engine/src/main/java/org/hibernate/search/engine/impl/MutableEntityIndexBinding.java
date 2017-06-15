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
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.store.ShardIdentifierProvider;
import org.hibernate.search.indexes.impl.IndexManagerGroupHolder;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class MutableEntityIndexBinding implements EntityIndexBinding {

	private final IndexManagerGroupHolder indexManagerGroupHolder;
	private final IndexManagerSelector indexManagerSelector;
	private final ShardIdentifierProvider shardIdentifierProvider;
	private DocumentBuilderIndexedEntity documentBuilder;
	private final EntityIndexingInterceptor<?> entityIndexingInterceptor;

	public MutableEntityIndexBinding(
			IndexManagerGroupHolder indexManagerGroupHolder,
			IndexManagerSelector indexManagerSelector,
			ShardIdentifierProvider shardIdentifierProvider,
			EntityIndexingInterceptor<?> entityIndexingInterceptor) {
		this.indexManagerGroupHolder = indexManagerGroupHolder;
		this.indexManagerSelector = indexManagerSelector;
		this.shardIdentifierProvider = shardIdentifierProvider;
		this.entityIndexingInterceptor = entityIndexingInterceptor;
	}

	/**
	 * Allows to set the document builder for this {@code EntityIndexBinding}.
	 *
	 * @param documentBuilder the new document builder instance
	 */
	public void setDocumentBuilderIndexedEntity(DocumentBuilderIndexedEntity documentBuilder) {
		this.documentBuilder = documentBuilder;
	}

	@Override
	public Similarity getSimilarity() {
		return indexManagerGroupHolder.getSimilarity();
	}

	@Override
	public IndexManagerSelector getIndexManagerSelector() {
		return indexManagerSelector;
	}

	@Override
	public ShardIdentifierProvider getShardIdentifierProvider() {
		return shardIdentifierProvider;
	}

	@Override
	public DocumentBuilderIndexedEntity getDocumentBuilder() {
		return documentBuilder;
	}

	@Override
	public void postInitialize(IndexedTypeSet indexedClasses) {
		documentBuilder.postInitialize( indexedClasses );
	}

	@Override
	public IndexManagerType getIndexManagerType() {
		return indexManagerGroupHolder.getIndexManagerType();
	}

	@Override
	public IndexManager[] getIndexManagers() {
		Set<IndexManager> set = indexManagerSelector.all();
		return set.toArray( new IndexManager[set.size()] );
	}

	@Override
	public EntityIndexingInterceptor<?> getEntityIndexingInterceptor() {
		return entityIndexingInterceptor;
	}

}
