/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;

class ElasticsearchSearchResultImpl<T> extends SimpleSearchResult<T>
		implements ElasticsearchSearchResult<T> {
	ElasticsearchSearchResultImpl(long hitCount, List<T> hits) {
		super( hitCount, hits );
	}
}
