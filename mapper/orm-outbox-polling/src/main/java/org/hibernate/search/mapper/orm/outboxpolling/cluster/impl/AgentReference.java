/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cluster.impl;

import java.util.UUID;

public class AgentReference {
	public static AgentReference of(UUID id, String name) {
		return new AgentReference( id, name );
	}

	public final UUID id;
	public final String name;

	private AgentReference(UUID id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String toString() {
		return id + " - " + name;
	}
}
