/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import static org.assertj.core.api.Assertions.fail;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;

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
			fail( "Unexpected interruption " + e.getMessage() );
		}
	}
}
