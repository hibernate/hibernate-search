/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.engine.impl;

import java.util.Set;

import org.apache.lucene.search.Similarity;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexMapping;
import org.hibernate.search.indexes.IndexManager;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.query.collector.impl.FieldCacheCollectorFactory;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.util.impl.ScopedAnalyzer;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class MutableEntityIndexMapping<T> implements EntityIndexMapping<T> {

	private final IndexShardingStrategy shardingStrategy;
	private final Similarity similarityInstance;
	private DocumentBuilderIndexedEntity<T> documentBuilder;
	private final IndexManager[] providers;

	/**
	 * @param shardingStrategy
	 * @param similarityInstance
	 * @param providers
	 */
	public MutableEntityIndexMapping(IndexShardingStrategy shardingStrategy, Similarity similarityInstance, IndexManager[] providers) {
				this.shardingStrategy = shardingStrategy;
				this.similarityInstance = similarityInstance;
				this.providers = providers;
	}

	public void setDocumentBuilderIndexedEntity(DocumentBuilderIndexedEntity<T> documentBuilder) {
		this.documentBuilder = documentBuilder;
	}

	@Override
	public Similarity getSimilarity() {
		return similarityInstance;
	}

	@Override
	public IndexShardingStrategy getSelectionStrategy() {
		return shardingStrategy;
	}

	@Override
	public DocumentBuilderIndexedEntity<T> getDocumentBuilder() {
		return documentBuilder;
	}

	@Override
	public FieldCacheCollectorFactory getIdFieldCacheCollectionFactory() {
		//TODO remove this stuff from the DocumentBuilder, bring it here.
		return documentBuilder.getIdFieldCacheCollectionFactory();
	}

	@Override
	public void postInitialize(Set<Class<?>> indexedClasses) {
		documentBuilder.postInitialize( indexedClasses );
	}

	@Override
	public DirectoryProvider[] getDirectoryProviders() {
		DirectoryProvider[] dps = new DirectoryProvider[providers.length];
		for ( int i = 0; i < providers.length; i++ ) {
			DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) providers[i];
			dps[i] = indexManager.getDirectoryProvider();
		}
		return dps;
	}

}
