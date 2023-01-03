/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl;

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
