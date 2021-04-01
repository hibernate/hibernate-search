/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.massindexing.impl;

import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingMappingContext;
import org.hibernate.search.mapper.javabean.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoDefaultMassIndexer;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingContext;
import org.hibernate.search.mapper.javabean.massindexing.loader.JavaBeanIndexingOptions;

public class JavaBeanMassIndexer implements MassIndexer, JavaBeanIndexingOptions {

	private final PojoDefaultMassIndexer<JavaBeanIndexingOptions> delegate;
	private final DetachedBackendSessionContext sessionContext;

	private int objectLoadingBatchSize = 10;

	public JavaBeanMassIndexer(MassIndexingContext<JavaBeanIndexingOptions> massIndexingContext,
			MassIndexingMappingContext mappingContext,
			DetachedBackendSessionContext sessionContext,
			Set<? extends PojoRawTypeIdentifier<?>> targetedIndexedTypes,
			PojoScopeSchemaManager scopeSchemaManager,
			PojoScopeWorkspace scopeWorkspace) {
		delegate = new PojoDefaultMassIndexer<>( this,
				massIndexingContext, mappingContext,
				targetedIndexedTypes, scopeSchemaManager, scopeWorkspace );
		this.sessionContext = sessionContext;
	}

	@Override
	public String tenantIdentifier() {
		return sessionContext.tenantIdentifier();
	}

	@Override
	public MassIndexer typesToIndexInParallel(int threadsToIndexObjects) {
		delegate.typesToIndexInParallel( threadsToIndexObjects );
		return this;
	}

	@Override
	public MassIndexer threadsToLoadObjects(int numberOfThreads) {
		delegate.threadsToLoadObjects( numberOfThreads );
		return this;
	}

	@Override
	public MassIndexer batchSizeToLoadObjects(int batchSize) {
		if ( batchSize < 1 ) {
			throw new IllegalArgumentException( "batchSize must be at least 1" );
		}
		this.objectLoadingBatchSize = batchSize;
		return this;
	}

	@Override
	public int batchSizeToLoadObjects() {
		return objectLoadingBatchSize;
	}

	@Override
	public MassIndexer mergeSegmentsOnFinish(boolean enable) {
		delegate.mergeSegmentsOnFinish( enable );
		return this;
	}

	@Override
	public MassIndexer mergeSegmentsAfterPurge(boolean enable) {
		delegate.mergeSegmentsAfterPurge( enable );
		return this;
	}

	@Override
	public MassIndexer dropAndCreateSchemaOnStart(boolean dropAndCreateSchema) {
		delegate.dropAndCreateSchemaOnStart( dropAndCreateSchema );
		return this;
	}

	@Override
	public MassIndexer purgeAllOnStart(boolean purgeAll) {
		delegate.purgeAllOnStart( purgeAll );
		return this;
	}

	@Override
	public CompletionStage start() {
		return delegate.start();
	}

	@Override
	public void startAndWait() throws InterruptedException {
		delegate.startAndWait();
	}

	@Override
	public MassIndexer monitor(MassIndexingMonitor monitor) {
		delegate.monitor( monitor );
		return this;
	}

	@Override
	public MassIndexer failureHandler(MassIndexingFailureHandler failureHandler) {
		delegate.failureHandler( failureHandler );
		return this;
	}

}
