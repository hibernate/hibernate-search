/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	default DataClearConfig clearDatabaseData(boolean clear) {
		return clearDatabaseData( clear ? ClearDatabaseData.AUTOMATIC : ClearDatabaseData.DISABLED );
	}

	DataClearConfig clearDatabaseData(ClearDatabaseData clear);

	DataClearConfig manualDatabaseCleanup(Consumer<Session> cleanupAction);

	enum ClearDatabaseData {
		DISABLED, MANUAL, AUTOMATIC
	}
}
