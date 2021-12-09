/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

public interface TckBackendHelper {

	TckBackendFeatures getBackendFeatures();

	TckBackendSetupStrategy<?> createDefaultBackendSetupStrategy();

	TckBackendSetupStrategy<?> createMultiTenancyBackendSetupStrategy();

	/**
	 * @return A setup strategy for {@link org.hibernate.search.integrationtest.backend.tck.analysis.AnalysisBuiltinIT}.
	 */
	TckBackendSetupStrategy<?> createAnalysisNotConfiguredBackendSetupStrategy();

	/**
	 * @return A setup strategy for {@link org.hibernate.search.integrationtest.backend.tck.analysis.AnalysisBuiltinOverrideIT}.
	 */
	TckBackendSetupStrategy<?> createAnalysisBuiltinOverridesBackendSetupStrategy();

	/**
	 * @return A setup strategy for {@link org.hibernate.search.integrationtest.backend.tck.analysis.AnalysisCustomIT}.
	 */
	TckBackendSetupStrategy<?> createAnalysisCustomBackendSetupStrategy();

	TckBackendSetupStrategy<?> createNoShardingBackendSetupStrategy();

	TckBackendSetupStrategy<?> createHashBasedShardingBackendSetupStrategy(int shardCount);

	TckBackendSetupStrategy<?> createRarePeriodicRefreshBackendSetupStrategy();

}
