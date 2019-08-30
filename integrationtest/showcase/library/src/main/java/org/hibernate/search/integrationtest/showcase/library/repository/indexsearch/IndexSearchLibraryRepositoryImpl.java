/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.repository.indexsearch;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.integrationtest.showcase.library.dto.LibraryFacetedSearchResult;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.common.data.Range;

import org.springframework.beans.factory.annotation.Autowired;

public class IndexSearchLibraryRepositoryImpl implements IndexSearchLibraryRepository {

	@Autowired
	private EntityManager entityManager;

	@Override
	public List<Library> search(String terms, int limit, int offset) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}
		return Search.session( entityManager )
				.search( Library.class )
				.predicate( f -> f.match().field( "name" ).matching( terms ) )
				.sort( f -> f.field( "collectionSize" ).desc()
						.then().field( "name_sort" )
				)
				.fetchHits( limit, offset );
	}

	@Override
	public LibraryFacetedSearchResult searchFaceted(String terms, Integer minCollectionSize,
			List<LibraryServiceOption> libraryServices, int limit, int offset) {
		AggregationKey<Map<Range<Integer>, Long>> aggByCollectionSizekey = AggregationKey.of( "collectionSize" );
		AggregationKey<Map<LibraryServiceOption, Long>> aggByLibraryServiceKey = AggregationKey.of( "libraryService" );
		SearchResult<Library> result = Search.session( entityManager )
				.search( Library.class )
				.predicate( f -> f.bool( b -> {
					b.must( f.matchAll() ); // Match all libraries by default
					// Match query
					if ( terms != null && !terms.isEmpty() ) {
						b.must( f.match()
								.field( "name" )
								.matching( terms )
						);
					}
					if ( minCollectionSize != null ) {
						b.must( f.range().field( "collectionSize" ).above( minCollectionSize ) );
					}
					// Nested query + must loop
					if ( libraryServices != null && !libraryServices.isEmpty() ) {
							for ( LibraryServiceOption service : libraryServices ) {
								b.must( f.match()
										.field( "services" )
										.matching( service )
								);
							}
					}
				} ) )
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
				.fetch( limit, offset );
		return new LibraryFacetedSearchResult(
				result.getTotalHitCount(), result.getHits(),
				result.getAggregation( aggByCollectionSizekey ),
				result.getAggregation( aggByLibraryServiceKey )
		);
	}
}
