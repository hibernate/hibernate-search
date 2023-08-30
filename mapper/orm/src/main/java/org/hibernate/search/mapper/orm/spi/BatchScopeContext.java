/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.spi;

import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;

public interface BatchScopeContext<T> {

	PojoScopeWorkspace pojoWorkspace(String tenantId);

}
