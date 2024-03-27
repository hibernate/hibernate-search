/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
