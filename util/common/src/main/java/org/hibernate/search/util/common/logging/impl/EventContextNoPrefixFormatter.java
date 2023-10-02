/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging.impl;

import org.hibernate.search.util.common.reporting.EventContext;

public final class EventContextNoPrefixFormatter {

	private final EventContext eventContext;

	public EventContextNoPrefixFormatter(EventContext eventContext) {
		this.eventContext = eventContext;
	}

	@Override
	public String toString() {
		return eventContext.render();
	}
}
