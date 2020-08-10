/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;

import org.junit.Assert;

public class SlowerLoadingListener extends BaseSessionEventListener {

	public static void registerSlowerLoadingListener(Session session, long delay) {
		session.unwrap( SessionImplementor.class ).addEventListeners( new SlowerLoadingListener( delay ) );
	}

	private final long delay;

	public SlowerLoadingListener(long delay) {
		this.delay = delay;
	}

	@Override
	public void jdbcPrepareStatementEnd() {
		try {
			Thread.sleep( delay );
		}
		catch (InterruptedException e) {
			Assert.fail( "Unexpected interruption " + e.getMessage() );
		}
	}
}
