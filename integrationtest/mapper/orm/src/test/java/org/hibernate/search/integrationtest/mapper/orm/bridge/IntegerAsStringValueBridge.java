/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bridge;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

public final class IntegerAsStringValueBridge implements ValueBridge<Integer, String> {

	public static final StaticCounters.Key INSTANCE_COUNTER_KEY = StaticCounters.createKey();
	public static final StaticCounters.Key CLOSE_COUNTER_KEY = StaticCounters.createKey();

	public IntegerAsStringValueBridge() {
		StaticCounters.get().increment( INSTANCE_COUNTER_KEY );
	}

	@Override
	public String toIndexedValue(Integer value) {
		return value == null ? null : value.toString();
	}

	@Override
	public void close() {
		StaticCounters.get().increment( CLOSE_COUNTER_KEY );
	}
}
