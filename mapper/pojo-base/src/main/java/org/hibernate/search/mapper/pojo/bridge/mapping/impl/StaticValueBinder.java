/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;

class StaticValueBinder<T> implements ValueBinder {
	private final Class<T> expectedValueType;
	private final ValueBridge<T, ?> bridge;

	StaticValueBinder(Class<T> expectedValueType, ValueBridge<T, ?> bridge) {
		this.expectedValueType = expectedValueType;
		this.bridge = bridge;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "expectedValueType=" + expectedValueType
				+ "bridge=" + bridge
				+ "]";
	}

	@Override
	public void bind(ValueBindingContext<?> context) {
		context.bridge( expectedValueType, bridge );
	}
}
