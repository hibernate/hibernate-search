/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.lucene.testsupport.configuration.DefaultITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneTestIndexesPathConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.rules.TestRule;

class LuceneTckBackendSetupStrategy implements TckBackendSetupStrategy {

	private final Map<String, Object> properties = new LinkedHashMap<>();

	LuceneTckBackendSetupStrategy() {
		setProperty( "type", "lucene" );
		setProperty( "directory.root", LuceneTestIndexesPathConfiguration.get().getPath()
				+ "/#{tck.startup.timestamp}/#{tck.test.id}/" );
		setProperty( "analysis.configurer", DefaultITAnalysisConfigurer.class.getName() );
	}

	LuceneTckBackendSetupStrategy setProperty(String key, Object value) {
		properties.put( key, value );
		return this;
	}

	@Override
	public Optional<TestRule> getTestRule() {
		return Optional.empty();
	}

	@Override
	public Map<String, ?> createBackendConfigurationProperties(TestConfigurationProvider configurationProvider) {
		return configurationProvider.interpolateProperties( properties );
	}

	@Override
	public TckBackendAccessor createBackendAccessor(TestConfigurationProvider configurationProvider) {
		Path indexesPath = Paths.get(
				(String) configurationProvider.interpolateProperties( properties ).get( "directory.root" )
		);
		return new LuceneTckBackendAccessor( indexesPath );
	}

	@Override
	public SearchSetupHelper.SetupContext startSetup(SearchSetupHelper.SetupContext setupContext) {
		return setupContext;
	}
}
