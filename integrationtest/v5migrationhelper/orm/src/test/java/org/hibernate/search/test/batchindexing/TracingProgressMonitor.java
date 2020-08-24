/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

public class TracingProgressMonitor implements MassIndexerProgressMonitor {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	@Override
	public void entitiesLoaded(int increment) {
		log.debugf( "entitiesLoaded(%d)", increment );
	}

	@Override
	public void documentsAdded(long increment) {
		log.debugf( "documentsAdded(%d)", increment );
	}

	@Override
	public void addToTotalCount(long increment) {
		log.debugf( "addToTotalCount(%d)", increment );
	}

	@Override
	public void indexingCompleted() {
		log.debug( "indexingCompleted()" );
	}

	@Override
	public void documentsBuilt(int number) {
		log.debugf( "documentsBuilt(%d)", number );
	}

}
