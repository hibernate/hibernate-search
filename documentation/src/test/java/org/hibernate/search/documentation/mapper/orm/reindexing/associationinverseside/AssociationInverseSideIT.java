/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.associationinverseside;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AssociationInverseSideIT {

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public AssociationInverseSideIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SEARCHABLE
				)
				.setup( Book.class, BookEdition.class );
	}

	@Test
	public void smoke() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
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

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.where( f -> f.bool()
							.must( f.match().field( "editionsForSale.label" ).matching( "paperback" ) )
							.must( f.match().field( "editionsForSale.label" ).matching( "kindle" ) )
					)
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 );
		} );
	}

}
