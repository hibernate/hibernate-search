/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.util.common.AssertionFailure;

public class SearchIndexingPlanExecutionReportImpl implements SearchIndexingPlanExecutionReport {

	public static SearchIndexingPlanExecutionReport from(IndexIndexingPlanExecutionReport<EntityReference> indexReport) {
		Throwable throwable = indexReport.getThrowable().orElse( null );
		List<EntityReference> failingEntities = indexReport.getFailingEntityReferences();
		if ( throwable == null && !failingEntities.isEmpty() ) {
			throwable = new AssertionFailure(
					"Unknown throwable: missing throwable when reporting the failure."
							+ " There is probably a bug in Hibernate Search, please report it."
			);
		}
		return new SearchIndexingPlanExecutionReportImpl( throwable, failingEntities );
	}

	private Throwable throwable;
	private List<EntityReference> failingEntities;

	private SearchIndexingPlanExecutionReportImpl(Throwable throwable, List<EntityReference> failingEntities) {
		this.throwable = throwable;
		this.failingEntities = failingEntities == null
				? Collections.emptyList() : Collections.unmodifiableList( failingEntities );
	}

	@Override
	public Optional<Throwable> throwable() {
		return Optional.ofNullable( throwable );
	}

	@Override
	public List<EntityReference> failingEntities() {
		return failingEntities;
	}

}
