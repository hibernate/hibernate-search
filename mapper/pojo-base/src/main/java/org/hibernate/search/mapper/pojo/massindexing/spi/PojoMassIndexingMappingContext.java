/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;

/**
 * Contextual information about a search mapping.
 */
public interface PojoMassIndexingMappingContext {

	/**
	 * @return A {@link EntityReferenceFactory} that relies on the object's class to return entity types.
	 */
	EntityReferenceFactory<?> entityReferenceFactory();

	/**
	 * @return A {@link ThreadPoolProvider}.
	 */
	ThreadPoolProvider threadPoolProvider();

	/**
	 * @return A {@link FailureHandler}.
	 */
	FailureHandler failureHandler();
}
