/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.function.Consumer;

import org.hibernate.Session;

public interface DataClearConfig {

	DataClearConfig tenants(Object... tenantIds);

	DataClearConfig preClear(Consumer<Session> preClear);

	<T> DataClearConfig preClear(Class<T> entityType, Consumer<T> preClear);

	DataClearConfig clearOrder(Class<?>... entityClasses);

	DataClearConfig clearIndexData(boolean clear);

	DataClearConfig clearDatabaseData(boolean clear);
}
