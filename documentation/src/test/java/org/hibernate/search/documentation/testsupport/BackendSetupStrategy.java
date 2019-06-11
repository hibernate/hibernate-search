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
	String DEFAULT_BACKEND_NAME = "backendName";

	default OrmSetupHelper.SetupContext withSingleBackend(OrmSetupHelper setupHelper) {
		return withBackend( setupHelper.startSetup(), DEFAULT_BACKEND_NAME )
				.withDefaultBackend( DEFAULT_BACKEND_NAME );
	}

	OrmSetupHelper.SetupContext withBackend(OrmSetupHelper.SetupContext setupContext, String backendName);

	static List<BackendSetupStrategy> simple() {
		return Arrays.asList(
				LUCENE,
				ELASTICSEARCH
		);
	}

	BackendSetupStrategy LUCENE = new BackendSetupStrategy() {
		@Override
		public String toString() {
			return "lucene";
		}
		@Override
		public OrmSetupHelper.SetupContext withBackend(OrmSetupHelper.SetupContext setupContext, String backendName) {
			return setupContext.withBackend( "backend-lucene", backendName )
					.withBackendProperty(
							backendName,
							LuceneBackendSettings.ANALYSIS_CONFIGURER,
							new LuceneSimpleMappingAnalysisConfigurer()
					);
		}
	};

	BackendSetupStrategy ELASTICSEARCH = new BackendSetupStrategy() {
		@Override
		public String toString() {
			return "elasticsearch";
		}
		@Override
		public OrmSetupHelper.SetupContext withBackend(OrmSetupHelper.SetupContext setupContext, String backendName) {
			return setupContext.withBackend( "backend-elasticsearch", backendName )
					.withBackendProperty(
							backendName,
							ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
							new ElasticsearchSimpleMappingAnalysisConfigurer()
					);
		}
	};
}
