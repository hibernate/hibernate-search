/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cluster.impl;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum AgentType {

	EVENT_PROCESSING_DYNAMIC_SHARDING,
	EVENT_PROCESSING_STATIC_SHARDING,
	MASS_INDEXING;

	public static final Set<AgentType> EVENT_PROCESSING =
			Collections.unmodifiableSet( EnumSet.of( EVENT_PROCESSING_DYNAMIC_SHARDING, EVENT_PROCESSING_STATIC_SHARDING ) );

}
