/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.routingkeybridge.ormcontext;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
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
public class RoutingKeyBridgeOrmContextIT {

	private static final int SHARD_COUNT = 4;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendSetups() {
		return BackendConfigurations.hashBasedSharding( SHARD_COUNT ).toArray();
	}

	@Rule
	public DocumentationSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public RoutingKeyBridgeOrmContextIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = DocumentationSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( MyEntity.class );
	}

	@Test
	public void smoke() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// See MyDataPropertyBinder
			entityManager.setProperty( "test.data.indexed", MyData.INDEXED );

			MyEntity myEntity = new MyEntity();
			entityManager.persist( myEntity );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<MyEntity> result = searchSession.search( MyEntity.class )
					.where( f -> f.matchAll() )
					.routing( "INDEXED" )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

}
