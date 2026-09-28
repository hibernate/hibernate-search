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
