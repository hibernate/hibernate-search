/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexedembedded.structure.flattened;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class IndexedEmbeddedStructureFlattenedIT {

	@Parameterized.Parameters(name = "{0}")
	public static List<?> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				BackendConfigurations.simple(),
				mapping -> {
					TypeMappingStep bookMapping = mapping.type( Book.class );
					bookMapping.indexed();
					bookMapping.property( "title" )
							.fullTextField().analyzer( "english" );
					bookMapping.property( "authors" )
							.indexedEmbedded().structure( ObjectStructure.FLATTENED );
					TypeMappingStep authorMapping = mapping.type( Author.class );
					authorMapping.property( "firstName" )
							.fullTextField().analyzer( "name" );
					authorMapping.property( "lastName" )
							.fullTextField().analyzer( "name" );
				} );
	}

	@Parameterized.Parameter
	@Rule
	public DocumentationSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, Author.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.setId( 1 );
			book.setTitle( "Leviathan Wakes" );

			Author author1 = new Author();
			author1.setId( 1 );
			author1.setFirstName( "Daniel" );
			author1.setLastName( "Abraham" );
			book.getAuthors().add( author1 );
			author1.getBooks().add( book );

			Author author2 = new Author();
			author2.setId( 2 );
			author2.setFirstName( "Ty" );
			author2.setLastName( "Frank" );
			book.getAuthors().add( author2 );
			author2.getBooks().add( book );

			entityManager.persist( author1 );
			entityManager.persist( author2 );
			entityManager.persist( book );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::include[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.and(
							f.match().field( "authors.firstName" ).matching( "Ty" ), // <1>
							f.match().field( "authors.lastName" ).matching( "Abraham" ) // <1>
					) )
					.fetchHits( 20 );

			assertThat( hits ).isNotEmpty(); // <2>
			// end::include[]
		} );
	}

}
