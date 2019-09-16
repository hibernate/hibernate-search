/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.index.settings.esnative.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.PutIndexSettingsWorkBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;


public class PutIndexSettingsWork extends AbstractSimpleElasticsearchWork<Void> {

	protected PutIndexSettingsWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder>
			implements PutIndexSettingsWorkBuilder {
		private final URLEncodedString indexName;
		private final JsonObject payload;

		public Builder(
				GsonProvider gsonProvider,
				URLEncodedString indexName, IndexSettings settings) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
			 * We better not include the null fields.
			 */
			Gson gson = gsonProvider.getGsonNoSerializeNulls();
			this.payload = gson.toJsonTree( settings ).getAsJsonObject();
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.put()
					.pathComponent( indexName )
					.pathComponent( Paths._SETTINGS )
					.body( payload );
			return builder.build();
		}

		@Override
		public PutIndexSettingsWork build() {
			return new PutIndexSettingsWork( this );
		}
	}
}