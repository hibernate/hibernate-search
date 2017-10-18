/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.function.Function;

import com.google.gson.JsonObject;

class TransformingHitExtractor<T, U> implements HitExtractor<U> {

	private final HitExtractor<T> delegate;
	private final Function<T, U> hitTransformer;

	public TransformingHitExtractor(HitExtractor<T> delegate, Function<T, U> hitTransformer) {
		this.delegate = delegate;
		this.hitTransformer = hitTransformer;
	}

	public void contributeRequest(JsonObject requestBody) {
		delegate.contributeRequest( requestBody );
	}

	public U extractHit(JsonObject responseBody, JsonObject hit) {
		T delegateResult = delegate.extractHit( responseBody, hit );
		return hitTransformer.apply( delegateResult );
	}

}
