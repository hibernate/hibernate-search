/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

public enum StubMappingSchemaManagementStrategy {

	NONE,
	DROP_AND_CREATE_AND_DROP,
	DROP_AND_CREATE_ON_STARTUP_ONLY,
	DROP_ON_SHUTDOWN_ONLY;

}
