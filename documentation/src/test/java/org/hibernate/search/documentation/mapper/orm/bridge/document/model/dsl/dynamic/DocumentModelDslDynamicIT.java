/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.document.model.dsl.dynamic;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
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
public class DocumentModelDslDynamicIT {
	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public DocumentModelDslDynamicIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyNames.SYNC
				)
				.setup( Book.class );
	}

	@Test
	public void smoke() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Book book1 = new Book();
			book1.setId( 1 );
			book1.getUserMetadata().put( "note", "I really liked this one" );
			book1.getUserMetadata().put( "recommended_audience", "Very very smart readers. Humble, too, of course." );
			book1.getMultiTypeUserMetadata().put( "comment", "Nice!" );
			book1.getMultiTypeUserMetadata().put( "rating_int", 84 );
			entityManager.persist( book1 );
			Book book2 = new Book();
			book2.setId( 2 );
			book2.getUserMetadata().put( "note", "I didn't like this one" );
			book2.getUserMetadata().put( "recommended_audience", "Dumb readers. Not me, of course." );
			book2.getMultiTypeUserMetadata().put( "comment", "Bad!" );
			book2.getMultiTypeUserMetadata().put( "rating_int", 55 );
			entityManager.persist( book2 );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result1 = searchSession.search( Book.class )
					.where( f -> f.match().field( "userMetadata.recommended_audience" )
							.matching( "smart" ) )
					.fetchHits( 20 );
			assertThat( result1 ).extracting( Book::getId )
					.containsExactly( 1 );

			List<Book> result2 = searchSession.search( Book.class )
					.where( f -> f.match().field( "multiTypeUserMetadata.comment" )
							.matching( "nice" ) )
					.fetchHits( 20 );
			assertThat( result2 ).extracting( Book::getId )
					.containsExactly( 1 );

			List<Book> result3 = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "multiTypeUserMetadata.rating_int" ).desc() )
					.fetchHits( 20 );
			assertThat( result3 ).extracting( Book::getId )
					.containsExactly( 1, 2 );

			List<Book> result4 = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "multiTypeUserMetadata.rating_int" ).asc() )
					.fetchHits( 20 );
			assertThat( result4 ).extracting( Book::getId )
					.containsExactly( 2, 1 );
		} );
	}

}
