/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import org.hibernate.query.SelectionQuery;

public interface OutboxEventPredicate {

	String queryPart(String eventAlias);

	void setParams(SelectionQuery<?> query);

}
