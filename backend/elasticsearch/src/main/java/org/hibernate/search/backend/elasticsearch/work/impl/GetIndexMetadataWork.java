/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExistingIndexMetadata;
import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GetIndexMetadataWork extends AbstractNonBulkableWork<List<ExistingIndexMetadata>> {

	private GetIndexMetadataWork(Builder builder) {
		super( builder );
	}

	@Override
	protected List<ExistingIndexMetadata> generateResult(ElasticsearchWorkExecutionContext context,
			ElasticsearchResponse response) {
		JsonObject body = response.body();
		List<ExistingIndexMetadata> result = new ArrayList<>();
		for ( Map.Entry<String, JsonElement> entry : body.entrySet() ) {
			JsonObject indexAsObject = entry.getValue().getAsJsonObject();

			IndexMetadata metadata = new IndexMetadata();
			metadata.setAliases( getAliases( context, indexAsObject ) );
			metadata.setSettings( getSettings( context, indexAsObject ) );
			metadata.setMapping( getMapping( context, indexAsObject ) );

			result.add( new ExistingIndexMetadata( entry.getKey(), metadata ) );
		}
		return result;
	}

	private Map<String, IndexAliasDefinition> getAliases(ElasticsearchWorkExecutionContext context, JsonObject index) {
		JsonElement aliases = index.get( "aliases" );
		if ( aliases == null || !aliases.isJsonObject() ) {
			throw new AssertionFailure(
					"Elasticsearch API call succeeded, but the aliases weren't mentioned in the result: " + index );
		}

		GsonProvider gsonProvider = context.getGsonProvider();
		Map<String, IndexAliasDefinition> result = new LinkedHashMap<>();
		for ( Map.Entry<String, JsonElement> entry : aliases.getAsJsonObject().entrySet() ) {
			IndexAliasDefinition aliasDefinition =
					gsonProvider.getGson().fromJson( entry.getValue(), IndexAliasDefinition.class );
			result.put( entry.getKey(), aliasDefinition );
		}
		return result;
	}

	private IndexSettings getSettings(ElasticsearchWorkExecutionContext context, JsonObject index) {
		JsonElement settings = index.get( "settings" );
		if ( settings == null || !settings.isJsonObject() ) {
			throw new AssertionFailure(
					"Elasticsearch API call succeeded, but the requested settings weren't mentioned in the result: " + index );
		}

		JsonElement indexSettings = settings.getAsJsonObject().get( "index" );
		if ( indexSettings != null ) {
			GsonProvider gsonProvider = context.getGsonProvider();
			return gsonProvider.getGson().fromJson( indexSettings, IndexSettings.class );
		}
		else {
			// Empty settings
			return new IndexSettings();
		}
	}

	private RootTypeMapping getMapping(ElasticsearchWorkExecutionContext context, JsonObject index) {
		JsonElement mappings = index.get( "mappings" );

		if ( mappings != null ) {
			GsonProvider gsonProvider = context.getGsonProvider();
			return gsonProvider.getGson().fromJson( mappings, RootTypeMapping.class );
		}
		else {
			return null;
		}
	}

	public static class Builder extends AbstractBuilder<Builder> {

		private final Set<URLEncodedString> indexNames = new LinkedHashSet<>();

		public static Builder create() {
			return new Builder();
		}

		private Builder() {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
		}

		public Builder index(URLEncodedString indexName) {
			indexNames.add( indexName );
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.get()
							.multiValuedPathComponent( indexNames );
			// This prevents the request from failing if the given index name does not match anything
			builder.param( "ignore_unavailable", true );
			// According to the documentation, this should prevent the request from failing
			// if the given index name does not match anything, but actually it has no effect.
			// Leaving it anyway in case they fix it someday.
			builder.param( "allow_no_indices", true );
			return builder.build();
		}

		@Override
		public GetIndexMetadataWork build() {
			return new GetIndexMetadataWork( this );
		}
	}
}
