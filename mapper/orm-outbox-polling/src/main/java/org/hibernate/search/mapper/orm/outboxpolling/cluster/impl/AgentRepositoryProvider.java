package org.hibernate.search.mapper.orm.outboxpolling.cluster.impl;

import org.hibernate.Session;

public interface AgentRepositoryProvider {

	AgentRepository create(Session session);

}
