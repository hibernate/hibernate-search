/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search;

import java.util.List;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.spi.AbstractSearchQuery;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;

final class StubSearchQuery<H> extends AbstractSearchQuery<H, SearchResult<H>>
		implements SearchQuery<H> {

	private final StubBackend backend;
	private final List<String> indexNames;
	private final StubSearchWork.Builder workBuilder;
	private final FromDocumentFieldValueConvertContext convertContext;
	private final LoadingContext<?, ?> loadingContext;
	private final StubSearchProjection<H> rootProjection;

	StubSearchQuery(StubBackend backend, List<String> indexNames, StubSearchWork.Builder workBuilder,
			FromDocumentFieldValueConvertContext convertContext,
			LoadingContext<?, ?> loadingContext, StubSearchProjection<H> rootProjection) {
		this.backend = backend;
		this.indexNames = indexNames;
		this.workBuilder = workBuilder;
		this.convertContext = convertContext;
		this.loadingContext = loadingContext;
		this.rootProjection = rootProjection;
	}

	@Override
	public String getQueryString() {
		return getClass().getName() + "@" + Integer.toHexString( hashCode() );
	}

	@Override
	public <Q> Q extension(SearchQueryExtension<Q, H> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, loadingContext )
		);
	}

	@Override
	public SearchResult<H> fetch(Integer limit, Integer offset) {
		workBuilder.limit( limit ).offset( offset );
		return backend.getBehavior().executeSearchWork(
				indexNames, workBuilder.build(), convertContext, loadingContext, rootProjection
		);
	}

	@Override
	public long fetchTotalHitCount() {
		return backend.getBehavior().executeCountWork( indexNames );
	}
}
