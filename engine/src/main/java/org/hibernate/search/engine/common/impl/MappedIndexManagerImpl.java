/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;

class MappedIndexManagerImpl<D extends DocumentElement> implements MappedIndexManager<D> {

	private final IndexManagerImplementor<D> implementor;

	MappedIndexManagerImpl(IndexManagerImplementor<D> implementor) {
		this.implementor = implementor;
	}

	@Override
	public IndexWorkPlan<D> createWorkPlan(SessionContext sessionContext) {
		return implementor.createWorkPlan( sessionContext );
	}

	@Override
	public IndexSearchTargetBuilder createSearchTarget() {
		return implementor.createSearchTarget();
	}

	@Override
	public void addToSearchTarget(IndexSearchTargetBuilder searchTargetBuilder) {
		implementor.addToSearchTarget( searchTargetBuilder );
	}
}
