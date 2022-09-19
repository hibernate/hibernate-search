/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.routingbridge.ormcontext;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RoutingBridgeOrmContextIT {

	private static final int SHARD_COUNT = 4;

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.hashBasedSharding( SHARD_COUNT ) );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( MyEntity.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// See MyDataPropertyBinder
			entityManager.setProperty( "test.data.indexed", MyData.INDEXED );

			MyEntity myEntity = new MyEntity();
			myEntity.setId( 1 );
			entityManager.persist( myEntity );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<MyEntity> result = searchSession.search( MyEntity.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// See MyDataPropertyBinder
			entityManager.setProperty( "test.data.indexed", MyData.NOT_INDEXED );

			MyEntity myEntity = entityManager.getReference( MyEntity.class, 1 );
			// Force the update, otherwise Hibernate Search will assume nothing changed
			Search.session( entityManager ).indexingPlan().addOrUpdate( myEntity );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<MyEntity> result = searchSession.search( MyEntity.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );

			assertThat( result ).isEmpty();
		} );
	}

}
