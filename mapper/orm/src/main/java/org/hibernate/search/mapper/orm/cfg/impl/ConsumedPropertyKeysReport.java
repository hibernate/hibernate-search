/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg.impl;

import java.util.Set;

public class ConsumedPropertyKeysReport {
	private final Set<String> availablePropertyKeys;
	private final Set<String> consumedPropertyKeys;

	ConsumedPropertyKeysReport(Set<String> availablePropertyKeys,
			Set<String> consumedPropertyKeys) {
		this.availablePropertyKeys = availablePropertyKeys;
		this.consumedPropertyKeys = consumedPropertyKeys;
	}

	public Set<String> getAvailablePropertyKeys() {
		return availablePropertyKeys;
	}

	public Set<String> getConsumedPropertyKeys() {
		return consumedPropertyKeys;
	}
}
