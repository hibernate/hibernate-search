/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.repository.indexsearch;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.integrationtest.showcase.library.dto.LibraryFacetedSearchResult;
import org.hibernate.search.integrationtest.showcase.library.dto.LibrarySimpleProjection;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.util.common.data.Range;

import org.springframework.beans.factory.annotation.Autowired;

public class IndexSearchLibraryRepositoryImpl implements IndexSearchLibraryRepository {

	@Autowired
	private EntityManager entityManager;

	@Override
	public List<Library> search(String terms, int offset, int limit) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}
		return Search.session( entityManager )
				.search( Library.class )
				.where( f -> f.match().field( "name" ).matching( terms ) )
				.sort( f -> f.field( "collectionSize" ).desc()
						.then().field( "name_sort" )
				)
				.fetchHits( offset, limit );
	}

	@Override
	public List<LibrarySimpleProjection> searchAndProject(String terms, int offset, int limit) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}
		return Search.session( entityManager )
				.search( Library.class )
				.select( LibrarySimpleProjection.class )
				.where( f -> f.match().field( "name" ).matching( terms ) )
				.sort( f -> f.field( "collectionSize" ).desc()
						.then().field( "name_sort" ) )
				.fetchHits( offset, limit );
	}

	@Override
	public List<LibrarySimpleProjection> searchAndProjectToMethodLocalClass(String terms, int offset, int limit) {
		@ProjectionConstructor
		class LocalClass {
			public final String name;
			public final List<LibraryServiceOption> services;

			public LocalClass(String name, List<LibraryServiceOption> services) {
				this.name = name;
				this.services = services;
			}
		}

		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}
		return Search.session( entityManager )
				.search( Library.class )
				.select( LocalClass.class )
				.where( f -> f.match().field( "name" ).matching( terms ) )
				.sort( f -> f.field( "collectionSize" ).desc()
						.then().field( "name_sort" ) )
				.fetchHits( offset, limit )
				.stream()
				.map( local -> new LibrarySimpleProjection( local.name, local.services ) )
				.collect( Collectors.toList() );
	}

	@Override
	public LibraryFacetedSearchResult searchFaceted(String terms, Integer minCollectionSize,
			List<LibraryServiceOption> libraryServices, int offset, int limit) {
		AggregationKey<Map<Range<Integer>, Long>> aggByCollectionSizekey = AggregationKey.of( "collectionSize" );
		AggregationKey<Map<LibraryServiceOption, Long>> aggByLibraryServiceKey = AggregationKey.of( "libraryService" );
		SearchResult<Library> result = Search.session( entityManager )
				.search( Library.class )
				.where( (f, root) -> {
					root.add( f.matchAll() ); // Match all libraries by default
					// Match query
					if ( terms != null && !terms.isEmpty() ) {
						root.add( f.match()
								.field( "name" )
								.matching( terms )
						);
					}
					if ( minCollectionSize != null ) {
						root.add( f.range().field( "collectionSize" ).atLeast( minCollectionSize ) );
					}
					// Nested query + must loop
					if ( libraryServices != null && !libraryServices.isEmpty() ) {
						for ( LibraryServiceOption service : libraryServices ) {
							root.add( f.match()
									.field( "services" )
									.matching( service )
							);
						}
					}
				} )
				.aggregation( aggByCollectionSizekey, f -> f.range()
						.field( "collectionSize", Integer.class )
						.range( 0, 1_000 )
						.range( 1_000, 5_000 )
						.range( 5_000, 10_000 )
						.range( 10_000, null )
				)
				.aggregation( aggByLibraryServiceKey, f -> f.terms()
						.field( "services", LibraryServiceOption.class )
						.orderByTermAscending()
						.minDocumentCount( 0 )
				)
				.fetch( offset, limit );
		return new LibraryFacetedSearchResult(
				result.total().hitCount(), result.hits(),
				result.aggregation( aggByCollectionSizekey ),
				result.aggregation( aggByLibraryServiceKey )
		);
	}
}
