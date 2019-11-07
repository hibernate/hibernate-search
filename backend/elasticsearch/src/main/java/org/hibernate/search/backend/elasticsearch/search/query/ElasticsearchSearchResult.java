/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query;

import java.time.Duration;

import org.hibernate.search.engine.search.query.SearchResult;

public interface ElasticsearchSearchResult<H> extends SearchResult<H> {

	/**
	 * @return the time the Elasticsearch server took to process the request, as a {@link Duration}
	 */
	Duration getTook();

	/**
	 * @return whether or not a timeout occurred on the server side processing the request.
	 */
	boolean isTimedOut();

}
