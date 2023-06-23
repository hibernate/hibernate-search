/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.time.Duration;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchScrollResult;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchScrollResult;

public class ElasticsearchSearchScrollResultImpl<H> extends SimpleSearchScrollResult<H>
		implements ElasticsearchSearchScrollResult<H> {
	public ElasticsearchSearchScrollResultImpl(SearchResultTotal total, boolean hasHits, List<H> hits,
			Duration took, Boolean timedOut) {
		super( total, hasHits, hits, took, timedOut );
	}
}
