/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.factory.impl;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.SearchWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.backend.elasticsearch.work.impl.SearchWork;

import com.google.gson.JsonObject;

/**
 * A work builder factory for ES6.0 to ES6.3.
 * <p>
 * Compared to ES6.3:
 * <ul>
 *     <li>We do NOT set the "allow_partial_search_results" query parameter in search APIs</li>
 * </ul>
 */
public class Elasticsearch60WorkBuilderFactory extends Elasticsearch63WorkBuilderFactory {

	public Elasticsearch60WorkBuilderFactory(GsonProvider gsonProvider) {
		super( gsonProvider );
	}

	@Override
	public <T> SearchWorkBuilder<T> search(JsonObject payload,
			ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		return SearchWork.Builder.forElasticsearch62AndBelow( payload, searchResultExtractor );
	}

}
