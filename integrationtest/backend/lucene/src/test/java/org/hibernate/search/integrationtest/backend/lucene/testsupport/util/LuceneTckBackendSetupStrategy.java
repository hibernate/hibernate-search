/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.configuration.DefaultITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

public class LuceneTckBackendSetupStrategy extends TckBackendSetupStrategy<LuceneBackendConfiguration> {

	public LuceneTckBackendSetupStrategy() {
		super( new LuceneBackendConfiguration() );
		setProperty( "analysis.configurer", BeanReference.ofInstance( new DefaultITAnalysisConfigurer() ) );
	}

	@Override
	public TckBackendAccessor createBackendAccessor(TestConfigurationProvider configurationProvider) {
		Path indexesPath = Paths.get(
				(String) configurationProvider.interpolateProperties( properties ).get( "directory.root" )
		);
		return new LuceneTckBackendAccessor( indexesPath );
	}
}
