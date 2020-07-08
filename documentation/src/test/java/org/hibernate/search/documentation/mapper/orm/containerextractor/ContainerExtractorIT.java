/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.containerextractor;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ContainerExtractorIT {

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public DocumentationSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public ContainerExtractorIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = DocumentationSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, Author.class );
	}

	@Test
	public void smoke() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Author author = new Author();
			author.setId( 1 );
			author.setName( "Isaac Asimov" );

			Book book = new Book();
			book.setId( 1 );
			book.setTitle( "The Caves Of Steel" );
			book.getAuthors().add( author );
			author.getBooks().add( book );
			book.getPriceByFormat().put( BookFormat.AUDIOBOOK, new BigDecimal( "15.99" ) );
			book.getPriceByFormat().put( BookFormat.HARDCOVER, new BigDecimal( "25.99" ) );

			entityManager.persist( author );
			entityManager.persist( book );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.where( f -> f.bool()
							.must( f.match().field( "availableFormats" ).matching( BookFormat.AUDIOBOOK ) )
							.must( f.match().field( "availableFormats" ).matching( BookFormat.HARDCOVER ) )
							.must( f.match().field( "authorCount" ).matching( 1, ValueConvert.NO ) )
					)
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 );
		} );
	}

}
