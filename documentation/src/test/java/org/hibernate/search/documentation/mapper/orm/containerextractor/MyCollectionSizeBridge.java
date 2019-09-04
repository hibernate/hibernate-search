/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.containerextractor;

import java.util.List;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public class MyCollectionSizeBridge implements ValueBridge<List<?>, Integer> {
	@Override
	public Integer toIndexedValue(List<?> value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.size();
	}
}
