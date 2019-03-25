/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;

public interface BackendSetupStrategy {
	OrmSetupHelper.SetupContext startSetup(OrmSetupHelper setupHelper);

	static List<BackendSetupStrategy> simple() {
		String backendName = "backendName";
		return Arrays.asList(
				new BackendSetupStrategy() {
					@Override
					public String toString() {
						return "lucene";
					}
					@Override
					public OrmSetupHelper.SetupContext startSetup(OrmSetupHelper setupHelper) {
						return setupHelper.withBackend( "lucene", backendName )
								.withBackendProperty(
										backendName,
										LuceneBackendSettings.ANALYSIS_CONFIGURER,
										new LuceneSimpleMappingAnalysisConfigurer()
								);
					}
				},
				new BackendSetupStrategy() {
					@Override
					public String toString() {
						return "elasticsearch";
					}
					@Override
					public OrmSetupHelper.SetupContext startSetup(OrmSetupHelper setupHelper) {
						return setupHelper.withBackend( "elasticsearch", backendName )
								.withBackendProperty(
										backendName,
										ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
										new ElasticsearchSimpleMappingAnalysisConfigurer()
								);
					}
				}
		);
	}
}
