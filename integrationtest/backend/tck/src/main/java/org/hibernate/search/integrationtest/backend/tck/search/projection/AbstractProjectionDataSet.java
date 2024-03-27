/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

public abstract class AbstractProjectionDataSet {

	protected final String routingKey;

	protected AbstractProjectionDataSet(String routingKey) {
		this.routingKey = routingKey;
	}

	@Override
	public String toString() {
		if ( routingKey != null ) {
			// Pretty rendering of the dataset as a parameter of the Parameterized runner
			return routingKey;
		}
		else {
			// Probably not used as a parameter of the Parameterized runner
			return getClass().getName();
		}
	}

}
