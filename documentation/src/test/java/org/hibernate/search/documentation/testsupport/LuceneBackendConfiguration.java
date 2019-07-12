/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;

public class LuceneBackendConfiguration extends AbstractDocumentationBackendConfiguration {
	@Override
	public String toString() {
		return "lucene";
	}

	@Override
	public <C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C setupWithName(C setupContext,
			String backendName, TestConfigurationProvider configurationProvider) {
		return setupContext
				.withBackendProperties(
						backendName,
						getBackendProperties( configurationProvider, "backend-lucene" )
				)
				.withBackendProperty(
						backendName,
						LuceneBackendSettings.ANALYSIS_CONFIGURER,
						new LuceneSimpleMappingAnalysisConfigurer()
				);
	}
}
