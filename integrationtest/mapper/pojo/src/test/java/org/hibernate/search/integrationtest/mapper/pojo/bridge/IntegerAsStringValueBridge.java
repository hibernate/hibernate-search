/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.bridge;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

public final class IntegerAsStringValueBridge implements ValueBridge<Integer, String> {

	@Override
	public Integer cast(Object value) {
		return (Integer) value;
	}

	@Override
	public String toIndexedValue(Integer value) {
		return value == null ? null : value.toString();
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
