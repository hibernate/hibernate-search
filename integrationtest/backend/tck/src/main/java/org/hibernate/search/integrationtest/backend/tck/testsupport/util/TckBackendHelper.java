/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;

public interface TckBackendHelper {

	TckBackendFeatures getBackendFeatures();

	TckBackendSetupStrategy<?> createDefaultBackendSetupStrategy();

	TckBackendSetupStrategy<?> createMultiTenancyBackendSetupStrategy();

	TckBackendSetupStrategy<?> createNoShardingMultiTenancyBackendSetupStrategy();

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

	/**
	 * @param f A {@link SearchPredicateFactory}
	 * @return A slow predicate, i.e. a predicate whose execution will take more than 10 milliseconds per document.
	 */
	PredicateFinalStep createSlowPredicate(SearchPredicateFactory f);

	<R, E, LOS> SearchQueryDslExtension<? extends SearchQuerySelectStep<?, R, E, LOS, ?, ?>, R, E, LOS> queryDslExtension();

}
