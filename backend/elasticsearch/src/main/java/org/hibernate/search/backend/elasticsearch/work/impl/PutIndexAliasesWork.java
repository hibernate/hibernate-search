/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.PutIndexAliasesWorkBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class PutIndexAliasesWork extends AbstractSimpleElasticsearchWork<Void> {

	protected PutIndexAliasesWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder>
			implements PutIndexAliasesWorkBuilder {
		private final JsonObject payload;

		public Builder(GsonProvider gsonProvider, URLEncodedString indexName,
				Map<String, IndexAliasDefinition> aliases) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.payload = createPayload( gsonProvider, indexName.original, aliases );
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
					.pathComponent( Paths._ALIASES )
					.body( payload );
			return builder.build();
		}

		@Override
		public PutIndexAliasesWork build() {
			return new PutIndexAliasesWork( this );
		}

		private static JsonObject createPayload(GsonProvider gsonProvider, String indexName,
				Map<String, IndexAliasDefinition> aliases) {
			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
			 * We better not include the null fields.
			 */
			Gson gson = gsonProvider.getGsonNoSerializeNulls();

			JsonObject payload = new JsonObject();
			JsonArray actions = new JsonArray();
			payload.add( "actions", actions );

			for ( Map.Entry<String, IndexAliasDefinition> entry : aliases.entrySet() ) {
				JsonObject action = new JsonObject();
				JsonObject aliasDefinition = gson.toJsonTree( entry.getValue() ).getAsJsonObject();
				action.add( "add", aliasDefinition );
				aliasDefinition.addProperty( "index", indexName );
				aliasDefinition.addProperty( "alias", entry.getKey() );

				actions.add( action );
			}

			return payload;
		}
	}
}