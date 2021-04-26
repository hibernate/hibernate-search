/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierSink;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingIdentifierLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoMassIndexingEntityIdentifierLoadingRunnable<E, I, O>
		extends PojoMassIndexingFailureHandledRunnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoMassIndexingIndexedTypeGroup<E, ?> typeGroup;
	private final PojoMassIndexingLoadingStrategy<E, I, O> loadingStrategy;
	private final O options;
	private final PojoProducerConsumerQueue<List<I>> identifierQueue;

	public PojoMassIndexingEntityIdentifierLoadingRunnable(PojoMassIndexingNotifier notifier,
			PojoMassIndexingIndexedTypeGroup<E, ?> typeGroup,
			PojoMassIndexingLoadingStrategy<E, I, O> loadingStrategy,
			O options, PojoProducerConsumerQueue<List<I>> identifierQueue) {
		super( notifier );
		this.loadingStrategy = loadingStrategy;
		this.typeGroup = typeGroup;
		this.options = options;
		this.identifierQueue = identifierQueue;
	}

	@Override
	protected void runWithFailureHandler() {
		log.trace( "started" );
		LoadingContext context = new LoadingContext();
		try ( PojoMassIdentifierLoader loader = loadingStrategy.createIdentifierLoader( context, options ) ) {
			long totalCount = loader.totalCount();
			getNotifier().notifyAddedTotalCount( totalCount );
			do {
				loader.loadNext();
			}
			while ( !context.done );
		}
		finally {
			identifierQueue.producerStopping();
		}
		log.trace( "finished" );
	}

	@Override
	protected void cleanUpOnFailure() {
		// Nothing to do
	}

	@Override
	protected void cleanUpOnInterruption() {
		// Nothing to do
	}

	@Override
	protected void notifyFailure(RuntimeException exception) {
		getNotifier().notifyRunnableFailure( exception,
				log.massIndexerFetchingIds( typeGroup.notifiedGroupName() ) );
	}

	private class LoadingContext implements PojoMassIndexingIdentifierLoadingContext<E, I> {
		private boolean done = false;

		@Override
		public Set<PojoRawTypeIdentifier<? extends E>> includedTypes() {
			return typeGroup.includedTypesIdentifiers();
		}

		@Override
		public PojoMassIdentifierSink<I> createSink() {
			return new PojoMassIdentifierSink<I>() {
				@Override
				public void accept(List<? extends I> batch) {
					try {
						log.tracef( "produced a list of ids %s", batch );
						List<I> copy = new ArrayList<>( batch );
						identifierQueue.put( copy );
					}
					catch (InterruptedException e) {
						// just quit
						complete();
						Thread.currentThread().interrupt();
					}
				}

				@Override
				public void complete() {
					done = true;
				}
			};
		}
	}
}
