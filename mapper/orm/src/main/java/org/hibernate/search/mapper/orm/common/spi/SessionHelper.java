/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.common.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * A helper to abstract away all the complexity of wrapping sections of code in a session (and transaction).
 * <p>
 * Particularly useful for unit testing of code that abstracts away from Hibernate ORM sessions.
 */
public final class SessionHelper {

	private final SessionFactoryImplementor sessionFactory;
	private final Object tenantId;

	public SessionHelper(SessionFactoryImplementor sessionFactory, Object tenantId) {
		this.sessionFactory = sessionFactory;
		this.tenantId = tenantId;
	}

	public SessionImplementor openSession() {
		return (SessionImplementor) sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession();
	}
}
