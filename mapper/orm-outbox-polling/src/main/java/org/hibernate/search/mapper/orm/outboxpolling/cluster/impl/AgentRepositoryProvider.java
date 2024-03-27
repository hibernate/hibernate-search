/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cluster.impl;

import org.hibernate.Session;

public interface AgentRepositoryProvider {

	AgentRepository create(Session session);

}
