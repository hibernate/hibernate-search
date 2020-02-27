/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl;

import java.util.Map;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.MultiValue;

class StubTermsAggregationBuilder<K>
		implements StubAggregationBuilder<Map<K, Long>>, TermsAggregationBuilder<K> {

	@Override
	public void orderByCountDescending() {
		// No-op
	}

	@Override
	public void orderByCountAscending() {
		// No-op
	}

	@Override
	public void orderByTermDescending() {
		// No-op
	}

	@Override
	public void orderByTermAscending() {
		// No-op
	}

	@Override
	public void minDocumentCount(int minDocumentCount) {
		// No-op
	}

	@Override
	public void maxTermCount(int maxTermCount) {
		// No-op
	}

	@Override
	public void multi(MultiValue multi) {
		// No-op
	}

	@Override
	public SearchAggregation<Map<K, Long>> build() {
		return new StubSearchAggregation<>( this );
	}
}
