/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.document.model.dsl.simple;


import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.Assertions;

@RunWith(Parameterized.class)
public class DocumentModelDslSimpleIT {
	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public DocumentModelDslSimpleIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SEARCHABLE
				)
				.setup( Author.class, Book.class );
	}

	@Test
	public void smoke() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Book book = new Book();
			book.setIsbn( ISBN.parse( "978-0-58-600835-5" ) );
			entityManager.persist( book );

			Author author = new Author();
			author.setFirstName( "Isaac" );
			author.setLastName( "Asimov" );
			entityManager.persist( author );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Object> result = searchSession.search( Arrays.asList( Book.class, Author.class ) )
					.predicate( f -> f.bool()
							.should( f.bool()
									.must( f.match().field( "fullName" )
											.matching( "isaac asimov" ) )
									.must( f.match().field( "names" )
											.matching( "isaac" ) )
									.must( f.match().field( "names" )
											.matching( "asimov" ) )
							)
							.should( f.match().field( "isbn" )
									.matching( "978-0-58-600835-5" ) )
					)
					.fetchHits( 20 );

			Assertions.assertThat( result ).hasSize( 2 );
		} );
	}

}
