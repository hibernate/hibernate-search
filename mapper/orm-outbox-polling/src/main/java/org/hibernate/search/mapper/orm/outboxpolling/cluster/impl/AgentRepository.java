/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cluster.impl;

import java.util.List;
import java.util.UUID;

public interface AgentRepository {

	Agent find(UUID id);

	List<Agent> findAllOrderById();

	void create(Agent agent);

	void delete(List<Agent> agents);

}
