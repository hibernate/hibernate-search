/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.util.Optional;

import org.hibernate.search.util.common.spi.ToStringTreeAppendable;

public abstract class OutboxEventFinderProvider implements ToStringTreeAppendable {

	@Override
	public String toString() {
		return toStringTree();
	}

	public abstract OutboxEventFinder create(Optional<OutboxEventPredicate> predicate);

}
