/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.dao.syntax.fluidandlambda;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;

import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.orm.jpa.FullTextQuery;
import org.hibernate.search.integrationtest.showcase.library.dao.DocumentDao;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryService;

class FluidAndLambdaSyntaxDocumentDao extends DocumentDao {
	FluidAndLambdaSyntaxDocumentDao(EntityManager entityManager) {
		super( entityManager );
	}

	@Override
	public Optional<Book> getByIsbn(String isbnAsString) {
		if ( isbnAsString == null ) {
			return Optional.empty();
		}

		// Must use Hibernate ORM types (as opposed to JPA types) to benefit from query.uniqueResult()
		FullTextSession fullTextSession = entityManager.unwrap( FullTextSession.class );

		org.hibernate.search.mapper.orm.hibernate.FullTextQuery<Book> query =
				fullTextSession.search( Book.class ).query()
				.asEntities()
				.predicate().match().onField( "isbn" ).matching( isbnAsString )
				.build();

		return Optional.ofNullable( query.uniqueResult() );
	}

	@Override
	public List<Book> searchByMedium(String terms, BookMedium medium, int offset, int limit) {
		FullTextQuery<Book> query = entityManager.search( Book.class ).query()
				.asEntities()
				.predicate().bool()
						.must( ctx -> {
							if ( terms != null && !terms.isEmpty() ) {
								ctx.match()
										.onField( "title" ).boostedTo( 2.0f )
										.orField( "summary" )
										.matching( terms );
							}
						} )
						.must().nested().onObjectField( "copies" )
								// Bridged query with function bridge: TODO rely on the bridge to convert to a String
								.match().onField( "copies.medium" ).matching( medium.name() )
						.end()
				.sort().byField( "title_sort" ).end()
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
				.asEntities()
				.predicate().bool()
						// Match query
						.must( ctx -> {
							if ( terms != null && !terms.isEmpty() ) {
								ctx.match()
										.onField( "title" ).boostedTo( 2.0f )
										.orField( "summary" )
										.matching( terms );
							}
						} )
						// Bridged query with complex bridge: TODO rely on the bridge to split the String
						.must( ctx -> {
							String[] splitTags = tags == null ? null : tags.split( "," );
							if ( splitTags != null && splitTags.length > 0 ) {
								ctx.bool().must( c2 -> {
									for ( String tag : splitTags ) {
										c2.match()
												.onField( "tags" )
												.matching( tag );
									}
								} );
							}
						} )
						// Spatial query
						// TODO spatial query
						/*
						.must( ctx -> {
							if ( myLocation != null && maxDistanceInKilometers != null ) {
								ctx.spatial()
										.onField( "copies.library.location" )
										.within( maxDistanceInKilometers, DistanceUnit.KM )
										.of( myLocation );
							}
						} );
						*/
						// Nested query + must loop
						.must( ctx -> {
							if ( libraryServices != null && !libraryServices.isEmpty() ) {
								ctx.nested().onObjectField( "copies" )
										.bool().must( c2 -> {
											for ( LibraryService service : libraryServices ) {
												c2.match()
														.onField( "copies.library.services" )
														// Bridged query with function bridge: TODO rely on the bridge to convert to a String
														.matching( service.name() );
											}
										} );
							}
						} )
						.end()
				// TODO facets (tag, medium, library in particular)
				.sort().byScore().end()
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}
}
