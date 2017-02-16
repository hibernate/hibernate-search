/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.work.impl.builder.DropIndexWorkBuilder;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.DeleteIndex;

/**
 * @author Yoann Rodiere
 */
public class DropIndexWork extends SimpleElasticsearchWork<JestResult, Void> {

	protected DropIndexWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, JestResult response) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult>
			implements DropIndexWorkBuilder {
		private final DeleteIndex.Builder jestBuilder;

		public Builder(String indexName) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new DeleteIndex.Builder( indexName );
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public DropIndexWork build() {
			return new DropIndexWork( this );
		}
	}
}