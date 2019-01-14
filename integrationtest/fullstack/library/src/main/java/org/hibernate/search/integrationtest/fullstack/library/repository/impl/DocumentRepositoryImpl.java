/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.repository.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.orm.jpa.FullTextQuery;
import org.hibernate.search.integrationtest.fullstack.library.repository.DocumentRepository;
import org.hibernate.search.integrationtest.fullstack.library.model.Book;
import org.hibernate.search.integrationtest.fullstack.library.model.BookMedium;
import org.hibernate.search.integrationtest.fullstack.library.model.Document;
import org.hibernate.search.integrationtest.fullstack.library.model.ISBN;
import org.hibernate.search.integrationtest.fullstack.library.model.LibraryService;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.orm.jpa.FullTextSearchTarget;

class DocumentRepositoryImpl extends DocumentRepository {

	@Override
	public Optional<Book> getByIsbn(String isbnAsString) {
		if ( isbnAsString == null ) {
			return Optional.empty();
		}

		// Must use Hibernate ORM types (as opposed to JPA types) to benefit from query.uniqueResult()
		FullTextSession fullTextSession = entityManager.unwrap( FullTextSession.class );

		org.hibernate.search.mapper.orm.hibernate.FullTextQuery<Book> query =
				fullTextSession.search( Book.class ).query()
				.asEntity()
				// TODO allow to bypass the bridge in the DSL
				.predicate( f -> f.match().onField( "isbn" ).matching( new ISBN( isbnAsString ) ).toPredicate() )
				.build();

		return Optional.ofNullable( query.uniqueResult() );
	}

	@Override
	public long count() {
		FullTextSession fullTextSession = entityManager.unwrap( FullTextSession.class );

		FullTextQuery<Book> query =
				fullTextSession.search( Book.class ).query()
						.asEntity()
						.predicate( f -> f.matchAll().toPredicate() )
						.build();

		return query.getResultSize();
	}

	@Override
	public List<Book> searchByMedium(String terms, BookMedium medium, int offset, int limit) {
		FullTextQuery<Book> query = entityManager.search( Book.class ).query()
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
			List<LibraryService> libraryServices,
			int offset, int limit) {
		FullTextQuery<Document<?>> query = entityManager.search( DOCUMENT_CLASS ).query()
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
									for ( LibraryService service : libraryServices ) {
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
		FullTextSearchTarget<Document> target = entityManager.search( Document.class );
		FullTextQuery<List<?>> query = target.query()
				.asProjections(
						target.projection().field( "author", String.class ).toProjection()
				)
				.predicate( f -> f.bool( b -> {
					b.must( f.match()
							.onField( "title" ).boostedTo( 2.0f )
							.orField( "summary" )
							.matching( terms )
					);
				} ) )
				.sort( b -> b.byField( "author" ).order( order ) )
				.build();

		// [ [ "Author 1" ], [ "Author 2" ], [ "Author 3" ] ] => [ "Author 1", "Author 2", "Author 3" ]
		return query.getResultList().stream().map( objects -> (String) objects.get( 0 ) ).collect( Collectors.toList() );
	}
}
