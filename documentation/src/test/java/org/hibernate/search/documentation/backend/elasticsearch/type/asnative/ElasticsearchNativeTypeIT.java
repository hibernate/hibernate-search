/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.elasticsearch.type.asnative;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.documentation.testsupport.ElasticsearchBackendConfiguration;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchNativeTypeIT {
	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( new ElasticsearchBackendConfiguration() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyNames.SYNC
				)
				.setup( CompanyServer.class );
	}

	@Test
	public void smoke() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			CompanyServer companyServer = new CompanyServer();
			companyServer.setIpAddress( "192.168.10.2" );
			entityManager.persist( companyServer );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<CompanyServer> result = searchSession.search( CompanyServer.class )
					.extension( ElasticsearchExtension.get() )
					.where( f -> f.fromJson(
							"{\n" +
							"  \"term\": {\n" +
							"    \"ipAddress\": \"192.168.0.0/16\"\n" +
							"  }\n" +
							"}"
					) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

}
