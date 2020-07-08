/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.identifierbridge.parameter;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class IdentifierBridgeParameterIT {
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
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	public void smoke() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Book book1 = new Book();
			book1.setId( 0 );
			book1.setTitle( "The Fellowship Of The Ring" );
			entityManager.persist( book1 );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.where( f -> f.id().matching( "1", ValueConvert.NO ) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

}
