/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubNextScrollWorkBehavior;

class SearchQueryEntityLoadingScrollingIT<T> extends SearchQueryEntityLoadingBaseIT<T> {

	@Override
	protected <T2> List<T2> getHits(List<String> targetIndexes, SearchQuery<T2> query,
			List<DocumentReference> hitDocumentReferences,
			Integer timeout, TimeUnit timeUnit) {
		backendMock.expectScrollObjects(
				targetIndexes,
				hitDocumentReferences.size(),
				b -> {
					if ( timeout != null && timeUnit != null ) {
						b.failAfter( timeout, timeUnit );
					}
				}
		);

		backendMock.expectNextScroll( targetIndexes, StubNextScrollWorkBehavior
				.of( hitDocumentReferences.size(), hitDocumentReferences ) );

		backendMock.expectCloseScroll( targetIndexes );

		try ( SearchScroll<T2> scroll = query.scroll( hitDocumentReferences.size() ) ) {
			SearchScrollResult<T2> next = scroll.next();
			return next.hits();
		}
	}
}
