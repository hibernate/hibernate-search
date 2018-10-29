/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.dao.syntax.lambda;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;

import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.orm.jpa.FullTextQuery;
import org.hibernate.search.integrationtest.showcase.library.dao.DocumentDao;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryService;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

class LambdaSyntaxDocumentDao extends DocumentDao {
	LambdaSyntaxDocumentDao(EntityManager entityManager) {
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
				// TODO allow to bypass the bridge in the DSL
				.predicate( root -> root.match().onField( "isbn" ).matching( new ISBN( isbnAsString ) ) )
				.build();

		return Optional.ofNullable( query.uniqueResult() );
	}

	@Override
	public List<Book> searchByMedium(String terms, BookMedium medium, int offset, int limit) {
		FullTextQuery<Book> query = entityManager.search( Book.class ).query()
				.asEntities()
				.predicate( root -> root.bool( b -> {
					b.must( ctx -> {
						if ( terms != null && !terms.isEmpty() ) {
							ctx.match()
									.onField( "title" ).boostedTo( 2.0f )
									.orField( "summary" )
									.matching( terms );
						}
					} );
					b.must( ctx -> ctx.nested().onObjectField( "copies" )
							.match().onField( "copies.medium" ).matching( medium )
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
				.asEntities()
				.predicate( root -> root.bool( b -> {
					// Match query
					b.must( ctx -> {
						if ( terms != null && !terms.isEmpty() ) {
							ctx.match()
									.onField( "title" ).boostedTo( 2.0f )
									.orField( "summary" )
									.matching( terms );
						}
					} );
					// Bridged query with complex bridge: TODO rely on the bridge to split the String
					b.must( ctx -> {
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
					} );
					// Spatial query
					b.must( ctx -> {
						if ( myLocation != null && maxDistanceInKilometers != null ) {
							ctx.nested().onObjectField( "copies" )
									.spatial()
									.within()
									.onField( "copies.library.location" )
									.circle( myLocation, maxDistanceInKilometers, DistanceUnit.KILOMETERS );
						}
					} );
					// Nested query + must loop
					b.must( ctx -> {
						if ( libraryServices != null && !libraryServices.isEmpty() ) {
							ctx.nested().onObjectField( "copies" )
									.bool().must( c2 -> {
								for ( LibraryService service : libraryServices ) {
									c2.match()
											.onField( "copies.library.services" )
											.matching( service );
								}
							} );
						}
					} );
				} ) )
				// TODO facets (tag, medium, library in particular)
				.sort( b -> b.byScore() )
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}
}
