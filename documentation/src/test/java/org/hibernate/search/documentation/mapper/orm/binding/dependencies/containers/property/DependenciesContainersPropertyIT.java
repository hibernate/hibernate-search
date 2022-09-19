/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.dependencies.containers.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DependenciesContainersPropertyIT {

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, BookEdition.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.setTitle( "The Caves Of Steel" );

			BookEdition edition1 = new BookEdition();
			edition1.setLabel( "Mass Market Paperback, 12th Edition" );
			edition1.setBook( book );
			book.getPriceByEdition().put( edition1, new BigDecimal( "25.99" ) );

			BookEdition edition2 = new BookEdition();
			edition2.setLabel( "Kindle Edition" );
			edition2.setBook( book );
			book.getPriceByEdition().put( edition2, new BigDecimal( "15.99" ) );

			entityManager.persist( edition1 );
			entityManager.persist( edition2 );
			entityManager.persist( book );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.where( f -> f.and(
							f.match().field( "editionsForSale" ).matching( "paperback" ),
							f.match().field( "editionsForSale" ).matching( "kindle" )
					) )
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 );
		} );
	}

}
