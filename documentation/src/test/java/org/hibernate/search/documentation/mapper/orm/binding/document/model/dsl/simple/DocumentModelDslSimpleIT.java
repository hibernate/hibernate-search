/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.document.model.dsl.simple;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DocumentModelDslSimpleIT {
	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Author.class, Book.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.setIsbn( ISBN.parse( "978-0-58-600835-5" ) );
			entityManager.persist( book );

			Author author = new Author();
			author.setFirstName( "Isaac" );
			author.setLastName( "Asimov" );
			entityManager.persist( author );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Object> result = searchSession.search( Arrays.asList( Book.class, Author.class ) )
					.where( f -> f.or(
							f.and(
									f.match().field( "fullName" )
											.matching( "isaac asimov" ),
									f.match().field( "names" )
											.matching( "isaac" ),
									f.match().field( "names" )
											.matching( "asimov" )
							),
							f.match().field( "isbn" )
									.matching( "978-0-58-600835-5" )
					) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 2 );
		} );
	}

}
