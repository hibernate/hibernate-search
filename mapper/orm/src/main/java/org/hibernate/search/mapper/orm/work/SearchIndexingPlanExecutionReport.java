/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.mapper.orm.common.EntityReference;

public interface SearchIndexingPlanExecutionReport {

	/**
	 * @return The {@link Exception} or {@link Error} thrown when indexing failed,
	 * or {@link Optional#empty()} if indexing succeeded.
	 */
	Optional<Throwable> throwable();

	/**
	 * @return The {@link Exception} or {@link Error} thrown when indexing failed,
	 * or {@link Optional#empty()} if indexing succeeded.
	 * @deprecated Use {@link #throwable()} instead.
	 */
	@Deprecated
	default Optional<Throwable> getThrowable() {
		return throwable();
	}

	/**
	 * @return A list of references to entities that may not be indexed correctly as a result of the failure.
	 * Never {@code null}, but may be empty.
	 */
	List<EntityReference> failingEntities();

	/**
	 * @return A list of references to entities that may not be indexed correctly as a result of the failure.
	 * Never {@code null}, but may be empty.
	 * @deprecated Use {@link #failingEntities()} instead.
	 */
	@Deprecated
	default List<EntityReference> getFailingEntities() {
		return failingEntities();
	}

}
