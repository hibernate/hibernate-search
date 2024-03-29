/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.sharding;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;

/**
 * A basic test for disabled sharding with explicit routing keys.
 */
class ShardingDisabledRoutingKeyIT extends AbstractShardingRoutingKeyIT {

	public ShardingDisabledRoutingKeyIT() {
		// Use more than one routing key
		super( TckBackendHelper::createNoShardingBackendSetupStrategy, generateRoutingKeys( 10 ) );
	}

}
