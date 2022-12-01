/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.DefaultITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

class ElasticsearchTckBackendSetupStrategy extends TckBackendSetupStrategy<ElasticsearchBackendConfiguration> {

	ElasticsearchTckBackendSetupStrategy() {
		super( new ElasticsearchBackendConfiguration() );
		setProperty( "analysis.configurer", BeanReference.ofInstance( new DefaultITAnalysisConfigurer() ) );
	}

	@Override
	public TckBackendAccessor createBackendAccessor(TestConfigurationProvider configurationProvider) {
		TestElasticsearchClient client = TestElasticsearchClient.create();
		client.open( configurationProvider );
		return new ElasticsearchTckBackendAccessor( client );
	}

}
