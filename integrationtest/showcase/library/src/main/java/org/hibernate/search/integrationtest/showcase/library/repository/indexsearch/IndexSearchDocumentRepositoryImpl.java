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
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryServiceOption;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.orm.jpa.FullTextQuery;
import org.hibernate.search.mapper.orm.jpa.FullTextSearchTarget;

import org.springframework.beans.factory.annotation.Autowired;

public class IndexSearchDocumentRepositoryImpl implements IndexSearchDocumentRepository {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Class<Document<?>> DOCUMENT_CLASS = (Class<Document<?>>) (Class) Document.class;

	private static final int MAX_RESULT = 10000;

	@Autowired
	private EntityManager entityManager;

	@Override
	public List<Book> findAllIndexed() {
		FullTextQuery<Book> query = Search.getFullTextEntityManager( entityManager )
				.search( Book.class )
				.query().asEntity()
				.predicate( p -> p.matchAll() )
				.build();

		query.setMaxResults( MAX_RESULT );
		return query.getResultList();
	}

	@Override
	public Optional<Book> getByIsbn(String isbnAsString) {
		if ( isbnAsString == null ) {
			return Optional.empty();
		}

		// Must use Hibernate ORM types (as opposed to JPA types) to benefit from query.uniqueResult()
		FullTextSession fullTextSession = Search.getFullTextEntityManager( entityManager ).unwrap( FullTextSession.class );

		org.hibernate.search.mapper.orm.hibernate.FullTextQuery<Book> query =
				fullTextSession.search( Book.class ).query()
						.asEntity()
						// TODO allow to bypass the bridge in the DSL
						.predicate( f -> f.match().onField( "isbn" ).matching( new ISBN( isbnAsString ) ) )
						.build();

		return Optional.ofNullable( query.uniqueResult() );
	}

	@Override
	public List<Book> searchByMedium(String terms, BookMedium medium, int offset, int limit) {
		FullTextQuery<Book> query = Search.getFullTextEntityManager( entityManager ).search( Book.class ).query()
				.asEntity()
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
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}

	@Override
	public List<Document<?>> searchAroundMe(String terms, String tags,
			GeoPoint myLocation, Double maxDistanceInKilometers,
			List<LibraryServiceOption> libraryServices,
			int offset, int limit) {
		FullTextQuery<Document<?>> query = Search.getFullTextEntityManager( entityManager ).search( DOCUMENT_CLASS ).query()
				.asEntity()
				.predicate( f -> f.bool( b -> {
					// Match query
					if ( terms != null && !terms.isEmpty() ) {
						b.must( f.match()
								.onField( "title" ).boostedTo( 2.0f )
								.orField( "summary" )
								.matching( terms )
						);
					}
					// Bridged query with complex bridge: TODO rely on the bridge to split the String
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
				// TODO facets (tag, medium, library in particular)
				.sort( b -> b.byScore() )
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}

	@Override
	public List<String> getAuthorsOfBooksHavingTerms(String terms, SortOrder order) {
		FullTextSearchTarget<Document> target = Search.getFullTextEntityManager( entityManager ).search( Document.class );
		FullTextQuery<String> query = target.query()
				.asProjection( f -> f.field( "author", String.class ) )
				.predicate( f -> f.match()
						.onField( "title" ).boostedTo( 2.0f )
						.orField( "summary" )
						.matching( terms )
				)
				.sort( b -> b.byField( "author" ).order( order ) )
				.build();

		return query.getResultList();
	}
}
