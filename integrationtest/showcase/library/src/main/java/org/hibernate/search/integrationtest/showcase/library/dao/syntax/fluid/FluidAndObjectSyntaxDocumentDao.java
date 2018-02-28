/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.dao.syntax.fluid;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;

import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.orm.jpa.FullTextQuery;
import org.hibernate.search.mapper.orm.jpa.FullTextSearchTarget;
import org.hibernate.search.integrationtest.showcase.library.dao.DocumentDao;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookMedium;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.LibraryService;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;

class FluidAndObjectSyntaxDocumentDao extends DocumentDao {
	FluidAndObjectSyntaxDocumentDao(EntityManager entityManager) {
		super( entityManager );
	}

	@Override
	public Optional<Book> getByIsbn(String isbnAsString) {
		if ( isbnAsString == null ) {
			return Optional.empty();
		}
		// Must use Hibernate ORM types (as opposed to JPA types) to benefit from query.uniqueResult()
		org.hibernate.search.mapper.orm.hibernate.FullTextQuery<Book> query =
				entityManager.unwrap( FullTextSession.class ).search( Book.class ).query()
				.asEntities()
				.predicate().match().onField( "isbn" ).matching( isbnAsString )
				.build();

		return Optional.ofNullable( query.uniqueResult() );
	}

	@Override
	public List<Book> searchByMedium(String terms, BookMedium medium, int offset, int limit) {
		FullTextSearchTarget<Book> target = entityManager.search( Book.class );
		BooleanJunctionPredicateContext<SearchPredicate> booleanBuilder = target.predicate().bool();

		if ( terms != null && !terms.isEmpty() ) {
			booleanBuilder.must().match()
					.onField( "title" ).boostedTo( 2.0f )
					.orField( "summary" )
					.matching( terms );
		}

		booleanBuilder.must( ctx -> ctx.nested().onObjectField( "copies" )
				// Bridged query with function bridge: TODO rely on the bridge to convert to a String
				.match().onField( "copies.medium" ).matching( medium.name() )
		);

		FullTextQuery<Book> query = entityManager.search( Book.class ).query()
				.asEntities()
				.predicate( booleanBuilder.end() )
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
		FullTextSearchTarget<Document<?>> target = entityManager.search( DOCUMENT_CLASS );
		BooleanJunctionPredicateContext<SearchPredicate> booleanBuilder = target.predicate().bool();

		// Match query
		if ( terms != null && !terms.isEmpty() ) {
			booleanBuilder.must().match()
					.onField( "title" ).boostedTo( 2.0f )
					.orField( "summary" )
					.matching( terms );
		}

		// Bridged query with complex bridge: TODO rely on the bridge to split the String
		String[] splitTags = tags == null ? null : tags.split( "," );
		if ( splitTags != null && splitTags.length > 0 ) {
			for ( String tag : splitTags ) {
				booleanBuilder.must().match()
						.onField( "tags" )
						.matching( tag );
			}
		}

		// Spatial query
		// TODO spatial query

		/*
		if ( myLocation != null && maxDistanceInKilometers != null ) {
			booleanBuilder.must().spatial()
					.onField( "copies.library.location" )
					.within( maxDistanceInKilometers, DistanceUnit.KM )
					.of( myLocation );
		}
		*/

		// Nested query + must loop
		if ( libraryServices != null && !libraryServices.isEmpty() ) {
			BooleanJunctionPredicateContext<?> nestedBoolean =
					booleanBuilder.must().nested().onObjectField( "copies" ).bool();
			for ( LibraryService service : libraryServices ) {
				nestedBoolean.must().match()
						.onField( "copies.library.services" )
						// Bridged query with function bridge: TODO rely on the bridge to convert to a String
						.matching( service.name() );
			}
		}

		FullTextQuery<Document<?>> query = target.query()
				.asEntities()
				.predicate( booleanBuilder.end() )
				// TODO facets (tag, medium, library in particular)
				.sort().byScore().end()
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}
}
