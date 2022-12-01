/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
