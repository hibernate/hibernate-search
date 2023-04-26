/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReferenceFactoryDelegate;

/**
 * Mapping-scoped information and operations for use in POJO work execution.
 */
public interface PojoWorkMappingContext extends BackendMappingContext, BridgeMappingContext {

	/**
	 * @return A {@link PojoEntityReferenceFactoryDelegate}.
	 */
	PojoEntityReferenceFactoryDelegate entityReferenceFactoryDelegate();

}
