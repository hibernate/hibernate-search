/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common.logging;

import org.hibernate.search.util.EventContext;

public class EventContextFormatter {

	private final String formatted;

	public EventContextFormatter(EventContext typeModel) {
		this.formatted = typeModel.render();
	}

	@Override
	public String toString() {
		return formatted;
	}
}
