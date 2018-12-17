/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.IndexSettings;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchLoadableSearchResult;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;

import com.google.gson.Gson;
import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchStubWorkFactory implements ElasticsearchWorkFactory {

	private final GsonProvider gsonProvider;

	public ElasticsearchStubWorkFactory(GsonProvider gsonProvider) {
		this.gsonProvider = gsonProvider;
	}

	@Override
	public ElasticsearchWork<?> createIndex(URLEncodedString indexName, URLEncodedString typeName,
			RootTypeMapping mapping,
			IndexSettings settings) {
		Gson gson = gsonProvider.getGsonNoSerializeNulls();

		JsonObject mappingMap = new JsonObject();
		mappingMap.add( typeName.original, gson.toJsonTree( mapping ) );

		JsonObject payload = new JsonObject();
		payload.add( "mappings", mappingMap );
		payload.add( "settings", gson.toJsonTree( settings ) );

		ElasticsearchRequest.Builder builder = ElasticsearchRequest.put()
				.pathComponent( indexName )
				.body( payload );
		return new ElasticsearchStubWork<>( builder.build() );
	}

	@Override
	public ElasticsearchWork<?> update(URLEncodedString indexName, URLEncodedString typeName,
			String id, String routingKey, JsonObject document) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.put()
				.pathComponent( indexName )
				.pathComponent( typeName )
				.pathComponent( URLEncodedString.fromString( id ) )
				.body( document );
		builder.param( "refresh", true );
		if ( routingKey != null ) {
			builder.param( "routing", routingKey );
		}
		return new ElasticsearchStubWork<>( builder.build() );
	}

	@Override
	public ElasticsearchWork<?> delete(URLEncodedString indexName, URLEncodedString typeName,
			String id, String routingKey) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.delete()
				.pathComponent( indexName )
				.pathComponent( typeName )
				.pathComponent( URLEncodedString.fromString( id ) );
		builder.param( "refresh", true );
		if ( routingKey != null ) {
			builder.param( "routing", routingKey );
		}
		return new ElasticsearchStubWork<>( builder.build() );
	}

	@Override
	public <T> ElasticsearchWork<ElasticsearchLoadableSearchResult<T>> search(Set<URLEncodedString> indexNames, Set<String> routingKeys,
			JsonObject payload, ElasticsearchSearchResultExtractor<T> searchResultExtractor,
			Long offset, Long limit) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.post()
				.multiValuedPathComponent( indexNames )
				.pathComponent( Paths._SEARCH )
				.body( payload );

		if ( offset != null ) {
			builder.param( "from", offset );
		}
		if ( limit != null ) {
			builder.param( "size", limit );
		}

		if ( !routingKeys.isEmpty() ) {
			builder.param( "routing", routingKeys.stream().collect( Collectors.joining( "," ) ) );
		}

		/* TODO scroll
		if ( scrollSize != null && scrollTimeout != null ) {
			builder.param( "size", scrollSize );
			builder.param( "scroll", scrollTimeout );
		}
		*/

		return new ElasticsearchStubWork<>( builder.build(), searchResultExtractor::extract );
	}

	@Override
	public ElasticsearchWork<Long> count(Set<URLEncodedString> indexNames, Set<String> routingKeys, JsonObject payload) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.post()
				.multiValuedPathComponent( indexNames )
				.pathComponent( Paths._COUNT )
				.body( payload );

		if ( !routingKeys.isEmpty() ) {
			builder.param( "routing", routingKeys.stream().collect( Collectors.joining( "," ) ) );
		}

		return new ElasticsearchStubWork<>( builder.build(), JsonAccessor.root().property( "count" ).asLong() );
	}
}
