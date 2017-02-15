/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.settings.GetSettings;

public class GetIndexSettingsWork extends SimpleElasticsearchWork<JestResult> {

	protected GetIndexSettingsWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final GetSettings.Builder jestBuilder;

		public Builder(String indexName) {
			super( null, DefaultElasticsearchRequestResultAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.jestBuilder = new GetSettings.Builder().addIndex( indexName );
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public GetIndexSettingsWork build() {
			return new GetIndexSettingsWork( this );
		}
	}
}