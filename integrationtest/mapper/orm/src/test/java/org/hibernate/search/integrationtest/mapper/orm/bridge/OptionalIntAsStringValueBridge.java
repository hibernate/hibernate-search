/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bridge;

import java.util.OptionalInt;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

public final class OptionalIntAsStringValueBridge implements ValueBridge<OptionalInt, String> {

	@Override
	public String toIndexedValue(OptionalInt value) {
		return value == null || !value.isPresent()
				? "empty"
				: String.valueOf( value.getAsInt() );
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
