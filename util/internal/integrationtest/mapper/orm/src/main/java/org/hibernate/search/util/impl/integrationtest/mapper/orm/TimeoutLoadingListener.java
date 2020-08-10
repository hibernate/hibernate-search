/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;

public class TimeoutLoadingListener extends BaseSessionEventListener {

	public static void registerTimingOutLoadingListener(Session session) {
		session.unwrap( SessionImplementor.class ).addEventListeners( new TimeoutLoadingListener() );
	}

	@Override
	public void jdbcPrepareStatementEnd() {
		throw new QueryTimeoutException( "Simulated timeout exception from the JBDC driver", null, "" );
	}
}
