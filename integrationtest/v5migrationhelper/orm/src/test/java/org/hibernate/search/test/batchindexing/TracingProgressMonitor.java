/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
