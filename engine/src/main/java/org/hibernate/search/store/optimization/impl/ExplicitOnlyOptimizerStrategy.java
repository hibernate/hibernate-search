/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.optimization.impl;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This OptimizerStrategy will only optimize the index when forced to,
 * using an explicit invocation to {@link org.hibernate.search.spi.SearchIntegrator#optimize()} or
 * {@link org.hibernate.search.spi.SearchIntegrator#optimize(Class)}
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class ExplicitOnlyOptimizerStrategy implements OptimizerStrategy {

	private static final Log log = LoggerFactory.make();

	protected String indexName;
	private final AtomicBoolean optimizerIsBusy = new AtomicBoolean();

	@Override
	public boolean performOptimization(IndexWriter writer) {
		boolean acquired = optimizerIsBusy.compareAndSet( false, true );
		if ( acquired ) {
			try {
				writer.forceMerge( 1, true );
				writer.commit();
			}
			catch (IOException e) {
				throw new SearchException( "Unable to optimize directoryProvider: " + indexName, e );
			}
			finally {
				optimizerIsBusy.set( false );
			}
			return true;
		}
		else {
			log.optimizationSkippedStillBusy( indexName );
			return false;
		}
	}

	@Override
	public void addOperationWithinTransactionCount(long operations) {
	}

	@Override
	public void optimize(Workspace workspace) {
	}

	@Override
	public void initialize(IndexManager indexManager, Properties indexProperties) {
		this.indexName = indexManager.getIndexName();
	}
}
