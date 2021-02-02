/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.spi;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;

public interface PojoScopeSessionContext extends BridgeSessionContext, BackendSessionContext {

	@Override
	PojoScopeMappingContext mappingContext();

}
