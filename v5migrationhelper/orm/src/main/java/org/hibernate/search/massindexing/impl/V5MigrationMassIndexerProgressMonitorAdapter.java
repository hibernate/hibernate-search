/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.massindexing.impl;

import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.mapper.orm.massindexing.MassIndexingMonitor;

public class V5MigrationMassIndexerProgressMonitorAdapter implements MassIndexingMonitor {
	private final MassIndexerProgressMonitor delegate;

	public V5MigrationMassIndexerProgressMonitorAdapter(MassIndexerProgressMonitor delegate) {
		this.delegate = delegate;
	}

	@Override
	public void documentsBuilt(long increment) {
		delegate.documentsBuilt( Math.toIntExact( increment ) );
	}

	@Override
	public void entitiesLoaded(long increment) {
		delegate.entitiesLoaded( Math.toIntExact( increment ) );
	}

	@Override
	public void addToTotalCount(long increment) {
		delegate.addToTotalCount( increment );
	}

	@Override
	public void indexingCompleted() {
		delegate.indexingCompleted();
	}

	@Override
	public void documentsAdded(long increment) {
		delegate.documentsAdded( increment );
	}
}
