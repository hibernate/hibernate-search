/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.List;

import org.hibernate.search.exception.AssertionFailure;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public abstract class SimpleBulkableElasticsearchWork<R>
		extends SimpleElasticsearchWork<R>
		implements BulkableElasticsearchWork<R> {

	private final JsonObject bulkableActionMetadata;

	protected SimpleBulkableElasticsearchWork(Builder<?> builder) {
		super( builder );
		this.bulkableActionMetadata = builder.buildBulkableActionMetadata();
	}

	@Override
	public void aggregate(ElasticsearchWorkAggregator aggregator) {
		aggregator.addBulkable( this );
	}

	@Override
	public JsonObject getBulkableActionMetadata() {
		return bulkableActionMetadata;
	}

	@Override
	public JsonObject getBulkableActionBody() {
		List<JsonObject> bodyParts = request.getBodyParts();
		if ( !bodyParts.isEmpty() ) {
			if ( bodyParts.size() > 1 ) {
				throw new AssertionFailure( "Found a bulkable action with multiple body parts: " + bodyParts );
			}
			return bodyParts.get( 0 );
		}
		else {
			return null;
		}
	}

	@Override
	public boolean handleBulkResult(ElasticsearchWorkExecutionContext context, JsonObject bulkResponseItem) {
		if ( resultAssessor.isSuccess( context, bulkResponseItem ) ) {
			afterSuccess( context );

			if ( markIndexDirty ) {
				context.setIndexDirty( dirtiedIndexName );
			}

			return true;
		}
		else {
			return false;
		}
	}

	protected abstract static class Builder<B>
			extends SimpleElasticsearchWork.Builder<B> {

		public Builder(String dirtiedIndexName, ElasticsearchRequestSuccessAssessor resultAssessor) {
			super( dirtiedIndexName, resultAssessor );
		}

		protected abstract JsonObject buildBulkableActionMetadata();

	}
}
