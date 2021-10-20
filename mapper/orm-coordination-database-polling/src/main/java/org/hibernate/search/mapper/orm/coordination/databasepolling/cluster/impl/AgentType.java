/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.cluster.impl;

public enum AgentType {

	EVENT_PROCESSING_DYNAMIC_SHARDING,
	EVENT_PROCESSING_STATIC_SHARDING
	// TODO HSEARCH-4358 add a type for mass indexer

}
