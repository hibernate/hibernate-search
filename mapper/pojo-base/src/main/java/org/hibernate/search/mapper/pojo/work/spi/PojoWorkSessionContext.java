/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorSessionContext;

/**
 * Session-scoped information and operations for use in POJO work execution.
 *
 * @param <R> The type of entity references.
 */
public interface PojoWorkSessionContext<R>
		extends BackendSessionContext, BridgeSessionContext, PojoIndexingProcessorSessionContext {

	@Override
	PojoWorkMappingContext getMappingContext();

	EntityReferenceFactory<R> getEntityReferenceFactory();

}
