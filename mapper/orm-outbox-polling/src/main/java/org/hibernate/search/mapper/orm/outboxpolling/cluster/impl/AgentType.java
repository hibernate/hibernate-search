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

public enum AgentType {

	EVENT_PROCESSING_DYNAMIC_SHARDING,
	EVENT_PROCESSING_STATIC_SHARDING,
	MASS_INDEXING;

	public static final Set<AgentType> EVENT_PROCESSING =
			Collections.unmodifiableSet( EnumSet.of( EVENT_PROCESSING_DYNAMIC_SHARDING, EVENT_PROCESSING_STATIC_SHARDING ) );

}
