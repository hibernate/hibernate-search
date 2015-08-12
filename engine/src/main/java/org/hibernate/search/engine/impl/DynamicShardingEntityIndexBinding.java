/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Properties;
import java.util.Set;

import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;

/**
 * @author Emmanuel Bernard
 */
public class DynamicShardingEntityIndexBinding implements MutableEntityIndexBinding {

	private final DynamicShardingStrategy shardingStrategy;
	private final Similarity similarityInstance;
	private final ShardIdentifierProvider shardIdentityProvider;
	private final Properties properties;
	private final ExtendedSearchIntegrator extendedIntegrator;
	private final IndexManagerHolder indexManagerHolder;
	private final String rootDirectoryProviderName;
	private DocumentBuilderIndexedEntity documentBuilder;
	private final EntityIndexingInterceptor entityIndexingInterceptor;
	private IndexManagerFactory indexManagerFactory;

	public DynamicShardingEntityIndexBinding(
			ShardIdentifierProvider shardIdentityProvider,
			Similarity similarityInstance,
			EntityIndexingInterceptor entityIndexingInterceptor,
			Properties properties,
			ExtendedSearchIntegrator extendedIntegrator,
			IndexManagerHolder indexManagerHolder,
			String rootDirectoryProviderName) {
		this.shardIdentityProvider = shardIdentityProvider;
		this.similarityInstance = similarityInstance;
		this.entityIndexingInterceptor = entityIndexingInterceptor;
		this.properties = properties;
		this.extendedIntegrator = extendedIntegrator;
		// TODO
		this.indexManagerFactory = indexManagerFactory;
		this.indexManagerHolder = indexManagerHolder;
		this.rootDirectoryProviderName = rootDirectoryProviderName;
		this.shardingStrategy = new DynamicShardingStrategy(
				shardIdentityProvider,
				indexManagerHolder,
				this,
				rootDirectoryProviderName
		);
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
	public IndexShardingStrategy getSelectionStrategy() {
		return shardingStrategy;
	}

	@Override
	public ShardIdentifierProvider getShardIdentifierProvider() {
		return shardingStrategy.getShardIdentifierProvider();
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
	public IndexManager[] getIndexManagers() {
		return shardingStrategy.getIndexManagersForAllShards();
	}

	@Override
	public EntityIndexingInterceptor getEntityIndexingInterceptor() {
		return entityIndexingInterceptor;
	}

	public Properties getProperties() {
		return properties;
	}

	public ExtendedSearchIntegrator getSearchintegrator() {
		return extendedIntegrator;
	}

	public IndexManagerFactory getIndexManagerFactory() {
		return indexManagerFactory;
	}

	public MutableEntityIndexBinding cloneWithSimilarity(Similarity entitySimilarity) {
		return new DynamicShardingEntityIndexBinding(
				shardIdentityProvider,
				entitySimilarity,
				entityIndexingInterceptor,
				properties,
				extendedIntegrator,
				indexManagerHolder,
				rootDirectoryProviderName
		);
	}
}
