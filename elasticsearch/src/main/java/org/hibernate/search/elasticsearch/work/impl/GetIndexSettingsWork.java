/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
import org.hibernate.search.elasticsearch.work.impl.builder.GetIndexSettingsWorkBuilder;
import org.hibernate.search.exception.AssertionFailure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.settings.GetSettings;

public class GetIndexSettingsWork extends SimpleElasticsearchWork<JestResult, IndexSettings> {

	private final String indexName;

	protected GetIndexSettingsWork(Builder builder) {
		super( builder );
		this.indexName = builder.indexName;
	}

	@Override
	protected IndexSettings generateResult(ElasticsearchWorkExecutionContext context, JestResult response) {
		JsonObject resultJson = response.getJsonObject();
		JsonElement index = resultJson.get( indexName );
		if ( index == null || !index.isJsonObject() ) {
			throw new AssertionFailure( "Elasticsearch API call succeeded, but the requested index wasn't mentioned in the result: " + resultJson );
		}

		JsonElement settings = index.getAsJsonObject().get( "settings" );
		if ( settings == null || !settings.isJsonObject() ) {
			throw new AssertionFailure( "Elasticsearch API call succeeded, but the requested settings weren't mentioned in the result: " + resultJson );
		}

		JsonElement indexSettings = settings.getAsJsonObject().get( "index" );
		if ( indexSettings != null ) {
			GsonService gsonService = context.getGsonService();
			return gsonService.getGson().fromJson( indexSettings, IndexSettings.class );
		}
		else {
			// Empty settings
			return new IndexSettings();
		}
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult>
			implements GetIndexSettingsWorkBuilder {
		private final String indexName;
		private final GetSettings.Builder jestBuilder;

		public Builder(String indexName) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.indexName = indexName;
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