/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.repository.indexsearch;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
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
	private static final Class<Document<?>> DOCUMENT_CLASS = (Class) Document.class;

	@Autowired
	private EntityManager entityManager;

	@Override
	public long countIndexed() {
		return Search.session( entityManager )
				.search( Document.class )
				.where( p -> p.matchAll() )
				.fetchTotalHitCount();
	}

	@Override
	public List<Document<?>> findAllIndexed() {
		return Search.session( entityManager )
				.search( DOCUMENT_CLASS )
				.where( p -> p.matchAll() )
				.fetchAllHits();
	}

	@Override
	public Optional<Book> getByIsbn(String isbnAsString) {
		if ( isbnAsString == null ) {
			return Optional.empty();
		}

		return Search.session( entityManager ).search( Book.class )
				// onRawField option allows to bypass the bridge in the DSL
				.where( f -> f.match().field( "isbn" ).matching( isbnAsString, ValueConvert.NO ) )
				.fetchSingleHit();
	}

	@Override
	public List<Book> searchByMedium(String terms, BookMedium medium, int offset, int limit) {
		return Search.session( entityManager ).search( Book.class )
				.where( (f, root) -> {
					if ( terms != null && !terms.isEmpty() ) {
						root.add( f.match()
								.field( "title" ).boost( 2.0f )
								.field( "summary" )
								.matching( terms )
						);
					}
					root.add( f.match().field( "copies.medium" ).matching( medium ) );
				} )
				.sort( b -> b.field( "title_sort" ) )
				.fetchHits( offset, limit );
	}

	@Override
	public List<Document<?>> searchAroundMe(String terms, String tags,
			GeoPoint myLocation, Double maxDistanceInKilometers,
			List<LibraryServiceOption> libraryServices,
			int offset, int limit) {
		return Search.session( entityManager ).search( DOCUMENT_CLASS )
				.where( (f, root) -> {
					// Match query
					if ( terms != null && !terms.isEmpty() ) {
						root.add( f.match()
								.field( "title" ).boost( 2.0f )
								.field( "summary" )
								.matching( terms )
						);
					}
					// Bridged query with complex bridge: TODO HSEARCH-3320 rely on the bridge to split the String
					String[] splitTags = tags == null ? null : tags.split( "," );
					if ( splitTags != null && splitTags.length > 0 ) {
						root.add( f.and().with( and -> {
							for ( String tag : splitTags ) {
								and.add( f.match()
										.field( "tags" )
										.matching( tag )
								);
							}
						} ) );
					}
					// Spatial query
					if ( myLocation != null && maxDistanceInKilometers != null ) {
						root.add( f.spatial()
								.within()
								.field( "copies.library.location" )
								.circle( myLocation, maxDistanceInKilometers, DistanceUnit.KILOMETERS )
						);
					}
					// Nested query + must loop
					if ( libraryServices != null && !libraryServices.isEmpty() ) {
						root.add( f.nested( "copies" )
								.with( b2 -> {
									for ( LibraryServiceOption service : libraryServices ) {
										b2.add( f.match()
												.field( "copies.library.services" )
												.matching( service )
										);
									}
								} )
						);
					}
				} )
				.sort( f -> f.composite( b -> {
					if ( myLocation != null ) {
						b.add( f.distance( "copies.library.location", myLocation ) );
					}
					b.add( f.score() );
				} ) )
				.fetchHits( offset, limit );
	}

	@Override
	public List<String> getAuthorsOfBooksHavingTerms(String terms, SortOrder order) {
		return Search.session( entityManager ).search( Document.class )
				.select( f -> f.field( "author", String.class ) )
				.where( f -> f.match()
						.field( "title" ).boost( 2.0f )
						.field( "summary" )
						.matching( terms )
				)
				.sort( b -> b.field( "author" ).order( order ) )
				.fetchAllHits();
	}

	@Override
	public void purge() {
		// This is faster than a workspace(...).purge(),
		// and works even on Amazon OpenSearch Serverless.
		Search.session( entityManager ).schemaManager( Document.class )
				.dropAndCreate();
	}
}
