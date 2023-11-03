/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
