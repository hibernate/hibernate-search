/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query;

import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.common.annotaion.Incubating;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchResult<H> extends SearchResult<H> {

	/**
	 * @return The body of the response to the search request as a {@link JsonObject}.
	 * The returned object must not be modified; use {@link JsonObject#deepCopy()} if necessary.
	 * <p>
	 * <strong>WARNING:</strong> The content of the response may change depending on
	 * the version of Elasticsearch, depending on which Hibernate Search features are used,
	 * and even depending on how Hibernate Search features are implemented.
	 * Thus this method cannot be guaranteed to return the same data when upgrading Hibernate Search,
	 * even for micro upgrades ({@code x.y.z} to {@code x.y.(z+1)}).
	 * Use this at your own risk.
	 */
	@Incubating
	JsonObject getResponseBody();

}
