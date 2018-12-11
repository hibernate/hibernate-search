/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.IndexSettings;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.util.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.engine.search.SearchResult;

import com.google.gson.Gson;
import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchStubWorkFactory implements ElasticsearchWorkFactory {

	private final GsonProvider gsonProvider;
	private final MultiTenancyStrategy multiTenancyStrategy;

	public ElasticsearchStubWorkFactory(GsonProvider gsonProvider, MultiTenancyStrategy multiTenancyStrategy) {
		this.gsonProvider = gsonProvider;
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	@Override
	public ElasticsearchWork<?> dropIndexIfExists(URLEncodedString indexName) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.delete()
				.pathComponent( indexName );
		return new ElasticsearchStubWork<>( builder.build() );
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
	public ElasticsearchWork<?> add(URLEncodedString indexName, URLEncodedString typeName,
			String id, String routingKey, JsonObject document) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.put()
				.pathComponent( indexName )
				.pathComponent( typeName )
				.pathComponent( URLEncodedString.fromString( id ) )
				.body( document );
		builder.param( "refresh", true );
		if ( routingKey != null ) {
			builder.param( "_routing", routingKey );
		}
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
			builder.param( "_routing", routingKey );
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
			builder.param( "_routing", routingKey );
		}
		return new ElasticsearchStubWork<>( builder.build() );
	}

	@Override
	public ElasticsearchWork<?> deleteAll(URLEncodedString indexName, String tenantId) {
		JsonObject matchAll = new JsonObject();
		matchAll.add( "match_all", new JsonObject() );
		JsonObject document = new JsonObject();
		document.add( "query", multiTenancyStrategy.decorateJsonQuery( matchAll, tenantId ) );

		ElasticsearchRequest.Builder builder = ElasticsearchRequest.post()
				.pathComponent( indexName )
				.pathComponent( Paths._DELETE_BY_QUERY )
				.param( "conflicts", "proceed" )
				.param( "refresh", true )
				.body( document );
		return new ElasticsearchStubWork<>( builder.build() );
	}

	@Override
	public ElasticsearchWork<?> flush(URLEncodedString indexName) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.post()
				.pathComponent( indexName )
				.pathComponent( Paths._FLUSH );
		ElasticsearchWork<?> flushWork = new ElasticsearchStubWork<>( builder.build() );
		builder = ElasticsearchRequest.post()
				.pathComponent( indexName )
				.pathComponent( Paths._REFRESH );
		ElasticsearchWork<Object> refreshWork = new ElasticsearchStubWork<>( builder.build() );
		return context -> flushWork.execute( context )
					.thenCompose( ignored -> refreshWork.execute( context ) );
	}

	@Override
	public ElasticsearchWork<?> optimize(URLEncodedString indexName) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.post()
				.pathComponent( indexName )
				.pathComponent( Paths._FORCEMERGE );
		return new ElasticsearchStubWork<>( builder.build() );
	}

	@Override
	public <T> ElasticsearchWork<SearchResult<T>> search(Set<URLEncodedString> indexNames, Set<String> routingKeys,
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
			builder.param( "_routing", routingKeys.stream().collect( Collectors.joining( "," ) ) );
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
			builder.param( "_routing", routingKeys.stream().collect( Collectors.joining( "," ) ) );
		}

		return new ElasticsearchStubWork<>( builder.build(), JsonAccessor.root().property( "count" ).asLong() );
	}
}
