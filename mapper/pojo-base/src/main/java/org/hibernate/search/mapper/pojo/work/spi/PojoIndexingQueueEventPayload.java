/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.io.Serializable;

import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

public final class PojoIndexingQueueEventPayload implements Serializable {

	public final DocumentRoutesDescriptor routes;
	public final DirtinessDescriptor dirtiness;

	public PojoIndexingQueueEventPayload(DocumentRoutesDescriptor routes,
			DirtinessDescriptor dirtiness) {
		this.routes = routes;
		this.dirtiness = dirtiness;
	}

	@Override
	public String toString() {
		return "PojoIndexingQueueEventPayload{" +
				"routes=" + routes +
				", updateCause=" + dirtiness +
				'}';
	}
}
