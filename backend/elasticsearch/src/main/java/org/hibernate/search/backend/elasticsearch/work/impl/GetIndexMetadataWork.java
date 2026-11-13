package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExistingIndexMetadata;
import org.hibernate.search.util.common.AssertionFailure;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class GetIndexMetadataWork extends AbstractNonBulkableWork<List<ExistingIndexMetadata>> {

	private GetIndexMetadataWork(Builder builder) {
		super( builder );
	}

	@Override
	protected List<ExistingIndexMetadata> generateResult(ElasticsearchWorkExecutionContext context,
			ElasticsearchJsonResponse response) {
		ObjectNode body = response.bodyJson();
		List<ExistingIndexMetadata> result = new ArrayList<>();
		for ( Map.Entry<String, JsonNode> entry : body.properties() ) {
			ObjectNode indexAsObject = ( (ObjectNode) entry.getValue() );
			IndexMetadata metadata = new IndexMetadata();
			metadata.setAliases( getAliases( context, indexAsObject ) );
			metadata.setSettings( getSettings( context, indexAsObject ) );
			metadata.setMapping( getMapping( context, indexAsObject ) );
			result.add( new ExistingIndexMetadata( entry.getKey(), metadata ) );
		}
		return result;
	}

	private Map<String, IndexAliasDefinition> getAliases(ElasticsearchWorkExecutionContext context, ObjectNode index) {
		JsonNode aliases = index.get( "aliases" );
		if ( aliases == null || !aliases.isObject() ) {
			throw new AssertionFailure(
					"Elasticsearch API call succeeded, but the aliases weren't mentioned in the result: " + index );
		}
		JsonProvider jsonProvider = context.getJsonProvider();
		Map<String, IndexAliasDefinition> result = new LinkedHashMap<>();
		for ( Map.Entry<String, JsonNode> entry : ( (ObjectNode) aliases ).properties() ) {
			IndexAliasDefinition aliasDefinition =
					jsonProvider.getCodec().decode( entry.getValue(), IndexAliasDefinition.class );
			result.put( entry.getKey(), aliasDefinition );
		}
		return result;
	}

	private IndexSettings getSettings(ElasticsearchWorkExecutionContext context, ObjectNode index) {
		JsonNode settings = index.get( "settings" );
		if ( settings == null || !settings.isObject() ) {
			throw new AssertionFailure(
					"Elasticsearch API call succeeded, but the requested settings weren't mentioned in the result: " + index );
		}
		JsonNode indexSettings = ( (ObjectNode) settings ).get( "index" );
		if ( indexSettings != null ) {
			JsonProvider jsonProvider = context.getJsonProvider();
			return jsonProvider.getCodec().decode( indexSettings, IndexSettings.class );
		}
		else {
			// Empty settings
			return new IndexSettings();
		}
	}

	private RootTypeMapping getMapping(ElasticsearchWorkExecutionContext context, ObjectNode index) {
		JsonNode mappings = index.get( "mappings" );
		if ( mappings != null ) {
			JsonProvider jsonProvider = context.getJsonProvider();
			return jsonProvider.getCodec().decode( mappings, RootTypeMapping.class );
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
		protected ElasticsearchJsonRequest buildRequest() {
			ElasticsearchJsonRequest.Builder builder = ElasticsearchJsonRequest.get().multiValuedPathComponent( indexNames );
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
