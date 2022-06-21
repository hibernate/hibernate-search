/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

public abstract class PojoIndexingAddOrUpdateOrDeleteScenario extends PojoIndexingOperationScenario {

	public PojoIndexingAddOrUpdateOrDeleteScenario(BackendIndexingOperation expectedBackendOperation) {
		super( expectedBackendOperation );
	}

	@Override
	final boolean expectImplicitLoadingOnNullEntity() {
		return true;
	}

	@Override
	final boolean expectSkipOnEntityAbsentAfterImplicitLoading() {
		return false;
	}

	@Override
	final <T> void addTo(SearchIndexingPlan indexingPlan, Object providedId, DocumentRoutesDescriptor providedRoutes,
			T entity) {
		throw notSupportedWithEntity();
	}

	@Override
	final <T> void addWithoutInstanceTo(SearchIndexingPlan indexingPlan, Class<T> entityClass, Object providedId,
			DocumentRoutesDescriptor providedRoutes) {
		indexingPlan.addOrUpdateOrDelete( entityClass, providedId, providedRoutes,
				// Tests in this package don't deal with dirty checking, so we take a shortcut here:
				// we assume everything is dirty.
				true, true );
	}

	@Override
	final CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes,
			IndexedEntity entity) {
		throw notSupportedOnIndexer();
	}

	@Override
	final CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes) {
		throw notSupportedOnIndexer();
	}

	private UnsupportedOperationException notSupportedWithEntity() {
		return new UnsupportedOperationException( "addOrUpdateOrDelete is not supported with an entity instance." );
	}

	private UnsupportedOperationException notSupportedOnIndexer() {
		return new UnsupportedOperationException( "addOrUpdateOrDelete is not supported on the PojoIndexer."
				+ " Tests are buggy, they should not be calling this method on this object." );
	}
}
