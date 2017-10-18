/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import com.google.gson.JsonObject;

class NullHitExtractor implements HitExtractor<Void> {

	private static final NullHitExtractor INSTANCE = new NullHitExtractor();

	public static NullHitExtractor get() {
		return INSTANCE;
	}

	private NullHitExtractor() {
		// Private constructor, use get() instead
	}

	@Override
	public void contributeRequest(JsonObject requestBody) {
		// Nothing to do
	}

	@Override
	public Void extractHit(JsonObject responseBody, JsonObject hit) {
		// Always return null
		return null;
	}
}
