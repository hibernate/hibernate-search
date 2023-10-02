/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import org.hibernate.query.Query;

public interface OutboxEventPredicate {

	String queryPart(String eventAlias);

	void setParams(Query<?> query);

}
