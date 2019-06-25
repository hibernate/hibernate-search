/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.repository.indexsearch;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;

import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.mapper.orm.Search;

import org.springframework.beans.factory.annotation.Autowired;

public class IndexSearchDocumentRepositoryImpl implements IndexSearchDocumentRepository {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Class<Document<?>> DOCUMENT_CLASS = (Class<Document<?>>) (Class) Document.class;

	@Autowired
	private EntityManager entityManager;

	@Override
	public long countIndexed() {
		return Search.session( entityManager )
				.search( Document.class )
				.predicate( p -> p.matchAll() )
				.fetchTotalHitCount();
	}

	@Override
	public Optional<Book> getByIsbn(String isbnAsString) {
		if ( isbnAsString == null ) {
			return Optional.empty();
		}

		return Search.session( entityManager ).search( Book.class )
				// onRawField option allows to bypass the bridge in the DSL
				.predicate( f -> f.match().onField( "isbn" ).matching( isbnAsString, DslConverter.DISABLED ) )
				.fetchSingleHit();
	}

	@Override
	public List<Book> searchByMedium(String terms, BookMedium medium, int limit, int offset) {
		return Search.session( entityManager ).search( Book.class )
				.predicate( f -> f.bool( b -> {
					if ( terms != null && !terms.isEmpty() ) {
						b.must( f.match()
								.onField( "title" ).boostedTo( 2.0f )
								.orField( "summary" )
								.matching( terms )
						);
					}
					b.must( f.nested().onObjectField( "copies" )
							.nest( f.match().onField( "copies.medium" ).matching( medium ) )
					);
				} ) )
				.sort( b -> b.byField( "title_sort" ) )
				.fetchHits( limit, offset );
	}

	@Override
	public List<Document<?>> searchAroundMe(String terms, String tags,
			GeoPoint myLocation, Double maxDistanceInKilometers,
			List<LibraryServiceOption> libraryServices,
			int limit, int offset) {
		return Search.session( entityManager ).search( DOCUMENT_CLASS )
				.predicate( f -> f.bool( b -> {
					// Match query
					if ( terms != null && !terms.isEmpty() ) {
						b.must( f.match()
								.onField( "title" ).boostedTo( 2.0f )
								.orField( "summary" )
								.matching( terms )
						);
					}
					// Bridged query with complex bridge: TODO HSEARCH-3320 rely on the bridge to split the String
					String[] splitTags = tags == null ? null : tags.split( "," );
					if ( splitTags != null && splitTags.length > 0 ) {
						b.must( f.bool( b2 -> {
							for ( String tag : splitTags ) {
								b2.must( f.match()
										.onField( "tags" )
										.matching( tag )
								);
							}
						} ) );
					}
					// Spatial query
					if ( myLocation != null && maxDistanceInKilometers != null ) {
						b.must( f.nested().onObjectField( "copies" )
								.nest( f.spatial()
										.within()
										.onField( "copies.library.location" )
										.circle( myLocation, maxDistanceInKilometers, DistanceUnit.KILOMETERS )
								)
						);
					}
					// Nested query + must loop
					if ( libraryServices != null && !libraryServices.isEmpty() ) {
						b.must( f.nested().onObjectField( "copies" )
								.nest( f.bool( b2 -> {
									for ( LibraryServiceOption service : libraryServices ) {
										b2.must( f.match()
												.onField( "copies.library.services" )
												.matching( service )
										);
									}
								} ) )
						);
					}
				} ) )
				// TODO HSEARCH-3271 facets (tag, medium, library in particular)
				.sort( f -> f.byComposite( b -> {
					if ( myLocation != null ) {
						// TODO HSEARCH-2254 sort by distance once we implement nested support for sorts ("copies" is a nested object field)
						//b.add( f.byDistance( "copies.library.location", myLocation ) );
					}
					b.add( f.byScore() );
				} ) )
				.fetchHits( limit, offset );
	}

	@Override
	public List<String> getAuthorsOfBooksHavingTerms(String terms, SortOrder order) {
		return Search.session( entityManager ).search( Document.class )
				.asProjection( f -> f.field( "author", String.class ) )
				.predicate( f -> f.match()
						.onField( "title" ).boostedTo( 2.0f )
						.orField( "summary" )
						.matching( terms )
				)
				.sort( b -> b.byField( "author" ).order( order ) )
				.fetchHits();
	}
}
