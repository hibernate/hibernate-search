/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.jta.timeout;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxPollingEventProcessor;

public class TimeoutSessionEventListener extends BaseSessionEventListener {

	@Override
	public void jdbcPrepareStatementEnd() {
		String name = Thread.currentThread().getName();
		if ( !name.contains( OutboxPollingEventProcessor.namePrefix( null ) ) ) {
			return;
		}

		try {
			// this should provoke a timeout in TransactionTimeoutJtaAndSpringOutboxIT test
			Thread.sleep( 1001 );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException( e );
		}
	}

}
