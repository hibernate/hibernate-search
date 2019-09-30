/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.logging.impl;

import org.hibernate.search.util.common.reporting.EventContext;

public class EventContextFormatter {

	private final String formatted;

	public EventContextFormatter(EventContext typeModel) {
		this.formatted = typeModel.renderWithPrefix();
	}

	@Override
	public String toString() {
		return formatted;
	}
}
