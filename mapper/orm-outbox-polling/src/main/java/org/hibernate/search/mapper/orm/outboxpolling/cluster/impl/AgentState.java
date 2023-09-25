/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.cluster.impl;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum AgentState {

	RUNNING,
	WAITING,
	SUSPENDED;

	public static final Set<AgentState> WAITING_OR_RUNNING =
			Collections.unmodifiableSet( EnumSet.of( WAITING, RUNNING ) );
}
