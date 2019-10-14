/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Throwables;

public class SearchIndexingPlanExecutionReportImpl implements SearchIndexingPlanExecutionReport {

	public static Function<IndexIndexingPlanExecutionReport, SearchIndexingPlanExecutionReport> factory(
			DocumentReferenceConverter<EntityReference> referenceConverter) {
		return report -> convert( referenceConverter, report );
	}

	private Throwable throwable;
	private List<EntityReference> failingEntities;

	private SearchIndexingPlanExecutionReportImpl(Throwable throwable, List<EntityReference> failingEntities) {
		this.throwable = throwable;
		this.failingEntities = failingEntities == null
				? Collections.emptyList() : Collections.unmodifiableList( failingEntities );
	}

	@Override
	public Optional<Throwable> getThrowable() {
		return Optional.ofNullable( throwable );
	}

	@Override
	public List<EntityReference> getFailingEntities() {
		return failingEntities;
	}

	private static SearchIndexingPlanExecutionReport convert(DocumentReferenceConverter<EntityReference> referenceConverter,
			IndexIndexingPlanExecutionReport indexReport) {
		Throwable throwable = indexReport.getThrowable().orElse( null );
		if ( throwable == null && !indexReport.getFailingDocuments().isEmpty() ) {
			throwable = new AssertionFailure(
					"Unknown throwable: missing throwable when reporting the failure."
							+ " There is probably a bug in Hibernate Search, please report it."
			);
		}
		List<EntityReference> failingEntities = null;
		for ( DocumentReference failingDocument : indexReport.getFailingDocuments() ) {
			if ( failingEntities == null ) {
				failingEntities = new ArrayList<>();
			}
			// If we get here, throwable is non-null.
			EntityReference reference = convertOrFail( referenceConverter, failingDocument, throwable );
			failingEntities.add( reference );
		}
		return new SearchIndexingPlanExecutionReportImpl( throwable, failingEntities );
	}

	private static EntityReference convertOrFail(DocumentReferenceConverter<EntityReference> referenceConverter,
			DocumentReference documentReference, Throwable throwable) {
		try {
			return referenceConverter.fromDocumentReference( documentReference );
		}
		catch (RuntimeException e) {
			// We failed to convert a reference.
			// Let's just give up and propagate the original exception, without the report.
			throwable.addSuppressed( e );
			throw Throwables.toRuntimeException( throwable );
		}
	}
}
