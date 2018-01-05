/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import com.google.gson.JsonObject;

public interface HitExtractor<C> {

	/**
	 * Contribute to the request, making sure that the requirements for this extractors are met.
	 */
	void contributeRequest(JsonObject requestBody);

	/**
	 * Perform hit extraction
	 *
	 * @param collector The hit collector, which will receive the result of the extraction.
	 * @param responseBody The full body of the response.
	 * @param hit The part of the response body relevant to the hit to extract.
	 */
	void extract(C collector, JsonObject responseBody, JsonObject hit);

}
