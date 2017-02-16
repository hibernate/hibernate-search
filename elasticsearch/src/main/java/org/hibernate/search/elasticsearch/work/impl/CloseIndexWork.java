/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.work.impl.builder.CloseIndexWorkBuilder;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.CloseIndex;

public class CloseIndexWork extends SimpleElasticsearchWork<JestResult, Void> {

	protected CloseIndexWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, JestResult response) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult>
			implements CloseIndexWorkBuilder {
		private final CloseIndex.Builder jestBuilder;

		public Builder(String indexName) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new CloseIndex.Builder( indexName );
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public CloseIndexWork build() {
			return new CloseIndexWork( this );
		}
	}
}