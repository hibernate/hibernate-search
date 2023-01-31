/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.massindexing.impl;

import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEnvironment;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingContext;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer;

public class StandalonePojoMassIndexer implements MassIndexer {

	private final PojoMassIndexer delegate;
	private final StandalonePojoLoadingContext context;

	public StandalonePojoMassIndexer(PojoMassIndexer delegate, StandalonePojoLoadingContext context) {
		this.delegate = delegate;
		this.context = context;
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
		context.batchSize( batchSize );
		return this;
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
	public CompletionStage<?> start() {
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

	@Override
	public <T> MassIndexer context(Class<T> contextType, T context) {
		this.context.context( contextType, context );
		return this;
	}

	@Override
	public MassIndexer environment(MassIndexingEnvironment environment) {
		delegate.environment( environment );
		return this;
	}
}
