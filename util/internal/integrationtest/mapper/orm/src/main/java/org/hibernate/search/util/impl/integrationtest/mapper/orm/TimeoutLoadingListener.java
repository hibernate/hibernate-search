/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.spi.SessionImplementor;

public class TimeoutLoadingListener implements SessionEventListener {

	public static void registerTimingOutLoadingListener(Session session) {
		session.unwrap( SessionImplementor.class ).addEventListeners( new TimeoutLoadingListener() );
	}

	@Override
	public void jdbcPrepareStatementEnd() {
		throw new QueryTimeoutException( "Simulated timeout exception from the JBDC driver", null, "" );
	}
}
