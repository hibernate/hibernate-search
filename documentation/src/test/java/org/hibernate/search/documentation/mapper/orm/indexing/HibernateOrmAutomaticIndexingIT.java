/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexing;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HibernateOrmAutomaticIndexingIT {
	private static final String BOOK1_TITLE = "I, Robot";

	private static final String BOOK2_TITLE = "The Caves of Steel";

	private static final String BOOK3_TITLE = "The Robots of Dawn";

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	public HibernateOrmAutomaticIndexingIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Test
	public void synchronizationStrategyOverride() {
		EntityManagerFactory entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						// To be overridden below
						AutomaticIndexingSynchronizationStrategyName.QUEUED
				)
				.setup( Book.class, Author.class );
		initData( entityManagerFactory );

		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			// tag::automatic-indexing-synchronization-strategy-override[]
			SearchSession searchSession = Search.session( entityManager ); // <1>
			searchSession.setAutomaticIndexingSynchronizationStrategy(
					AutomaticIndexingSynchronizationStrategy.searchable()
			); // <2>

			entityManager.getTransaction().begin();
			try {
				Book book = entityManager.find( Book.class, 1 );
				book.setTitle( book.getTitle() + " (2nd edition)" ); // <3>
				entityManager.getTransaction().commit(); // <4>
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
			}

			List<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.match().field( "title" ).matching( "2nd edition" ) )
					.fetchHits(); // <5>
			// end::automatic-indexing-synchronization-strategy-override[]

			assertThat( result ).extracting( Book::getId )
					.containsExactly( 1 );
		} );
	}

	private void initData(EntityManagerFactory entityManagerFactory) {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Book book1 = new Book();
			book1.setId( 1 );
			book1.setTitle( BOOK1_TITLE );
			Book book2 = new Book();
			book2.setId( 2 );
			book2.setTitle( BOOK2_TITLE );
			Book book3 = new Book();
			book3.setId( 3 );
			book3.setTitle( BOOK3_TITLE );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
		} );
	}

}
