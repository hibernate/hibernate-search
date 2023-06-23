/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.mapper.pojo.automaticindexing.spi.PojoImplicitReindexingResolverSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorSessionContext;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Session-scoped information and operations for use in POJO work execution.
 */
public interface PojoWorkSessionContext
		extends BackendSessionContext, BridgeSessionContext, PojoIndexingProcessorSessionContext,
		PojoImplicitReindexingResolverSessionContext {

	@Override
	PojoWorkMappingContext mappingContext();

	PojoSelectionLoadingContext defaultLoadingContext();

	@Incubating
	ConfiguredSearchIndexingPlanFilter configuredIndexingPlanFilter();

}
