/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.spi;

import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

/**
 * Session-scoped information and operations for use in POJO indexing processors.
 */
public interface PojoIndexingProcessorSessionContext extends BridgeSessionContext {

	PojoRuntimeIntrospector getRuntimeIntrospector();

}
