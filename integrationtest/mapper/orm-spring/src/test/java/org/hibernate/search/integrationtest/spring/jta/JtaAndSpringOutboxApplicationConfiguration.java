/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.jta;

import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;

public class JtaAndSpringOutboxApplicationConfiguration extends JtaAndSpringApplicationConfiguration {

	@Override
	public BackendMock backendMock() {
		BackendMock backendMock = super.backendMock();
		backendMock.indexingWorkExpectations( CoordinationStrategyExpectations.outboxPolling().indexingWorkExpectations );
		return backendMock;
	}
}
