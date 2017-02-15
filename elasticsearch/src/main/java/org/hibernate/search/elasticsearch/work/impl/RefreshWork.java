/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.Refresh;

/**
 * @author Yoann Rodiere
 */
public class RefreshWork extends SimpleElasticsearchWork<JestResult> {

	protected RefreshWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final Refresh.Builder jestBuilder;

		public Builder() {
			super( null, DefaultElasticsearchRequestResultAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new Refresh.Builder();
		}

		public Builder index(String indexName) {
			jestBuilder.addIndex( indexName );
			return this;
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public RefreshWork build() {
			return new RefreshWork( this );
		}
	}
}