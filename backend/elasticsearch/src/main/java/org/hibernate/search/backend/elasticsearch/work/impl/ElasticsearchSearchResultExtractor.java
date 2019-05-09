/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchLoadableSearchResult;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchResultExtractor<H> {

	ElasticsearchLoadableSearchResult<H> extract(JsonObject responseBody);

}
