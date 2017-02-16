/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;

import com.google.gson.Gson;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.settings.UpdateSettings;

/**
 * @author Yoann Rodiere
 */
public class PutIndexSettingsWork extends SimpleElasticsearchWork<JestResult, Void> {

	protected PutIndexSettingsWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, JestResult response) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final UpdateSettings.Builder jestBuilder;

		public Builder(
				GsonService gsonService,
				String indexName, IndexSettings settings) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
			 * We better not include the null fields.
			 */
			Gson gson = gsonService.getGsonNoSerializeNulls();
			String serializedSettings = gson.toJson( settings );
			this.jestBuilder = new UpdateSettings.Builder( serializedSettings ).addIndex( indexName );
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public PutIndexSettingsWork build() {
			return new PutIndexSettingsWork( this );
		}
	}
}