/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.identifierbridge.ormcontext;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class IdentifierBridgeOrmContextIT {
	@Parameterized.Parameters(name = "{0}")
	public static List<?> params() {
		return DocumentationSetupHelper.testParamsWithSingleBackend( BackendConfigurations.simple() );
	}

	@Parameterized.Parameter
	@Rule
	public DocumentationSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( MyEntity.class );
	}

	@Test
	public void smoke() {
		// See MyDataValueBridge
		entityManagerFactory.getProperties().put( "test.data.indexed", MyData.VALUE1 );
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			MyEntity myEntity = new MyEntity();
			myEntity.setId( MyData.VALUE3 );
			entityManager.persist( myEntity );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// See MyDataValueBridge
			entityManager.setProperty( "test.data.projected", MyData.VALUE2 );

			SearchSession searchSession = Search.session( entityManager );

			List<EntityReference> result = searchSession.search( MyEntity.class )
					.select( f -> f.entityReference() )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );

			assertThat( result ).extracting( EntityReference::id ).containsExactly( MyData.VALUE2 );
		} );
	}

}
