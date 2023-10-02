/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging.impl;

import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

public final class EventContextFormatter {

	private final EventContext eventContext;

	public EventContextFormatter(EventContextProvider eventContextProvider) {
		this( eventContextProvider.eventContext() );
	}

	public EventContextFormatter(EventContext eventContext) {
		this.eventContext = eventContext;
	}

	@Override
	public String toString() {
		return eventContext.renderWithPrefix();
	}
}
