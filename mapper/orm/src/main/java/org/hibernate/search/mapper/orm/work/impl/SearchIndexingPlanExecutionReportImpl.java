/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work.impl;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport;

public class SearchIndexingPlanExecutionReportImpl implements SearchIndexingPlanExecutionReport {

	private final MultiEntityOperationExecutionReport<EntityReference> delegate;

	public SearchIndexingPlanExecutionReportImpl(MultiEntityOperationExecutionReport<EntityReference> delegate) {
		this.delegate = delegate;
	}

	@Override
	public Optional<Throwable> throwable() {
		return delegate.throwable();
	}

	@Override
	public List<EntityReference> failingEntities() {
		return delegate.failingEntityReferences();
	}

}
