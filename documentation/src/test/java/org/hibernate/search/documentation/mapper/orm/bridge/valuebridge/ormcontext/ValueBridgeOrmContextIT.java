/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.valuebridge.ormcontext;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.engine.search.common.ValueConvert;
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
public class ValueBridgeOrmContextIT {
	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public ValueBridgeOrmContextIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SEARCHABLE
				)
				.setup( MyEntity.class );
	}

	@Test
	public void smoke() {
		// See MyDataValueBridge
		entityManagerFactory.getProperties().put( "test.data.indexed", MyData.INDEXED );
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			MyEntity myEntity = new MyEntity();
			entityManager.persist( myEntity );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// See MyDataValueBridge
			entityManager.setProperty( "test.data.projected", MyData.PROJECTED );

			SearchSession searchSession = Search.session( entityManager );

			List<MyData> result = searchSession.search( MyEntity.class )
					.asProjection( f -> f.field( "myData", MyData.class ) )
					.predicate( f -> f.match().field( "myData" )
							.matching( "INDEXED", ValueConvert.NO ) )
					.fetchHits( 20 );

			assertThat( result ).containsExactly( MyData.PROJECTED );
		} );
	}

}
