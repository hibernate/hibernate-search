/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReferenceFactoryDelegate;

/**
 * Contextual information about a search mapping.
 */
public interface PojoMassIndexingMappingContext extends BackendMappingContext {

	/**
	 * @return A {@link ThreadPoolProvider}.
	 */
	ThreadPoolProvider threadPoolProvider();

	/**
	 * @return A {@link FailureHandler}.
	 */
	FailureHandler failureHandler();

	/**
	 * Creates a {@link PojoMassIndexerAgent},
	 * able to exert control over other agents that could perform indexing concurrently (e.g. indexing plans).
	 *
	 * @param context A context with information about the mass indexing that is about to start.
	 * @return An agent.
	 */
	PojoMassIndexerAgent createMassIndexerAgent(PojoMassIndexerAgentCreateContext context);

	/**
	 * @return A {@link PojoEntityReferenceFactoryDelegate}.
	 */
	PojoEntityReferenceFactoryDelegate entityReferenceFactoryDelegate();

}
