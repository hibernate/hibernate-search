/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

public final class BoundValueBridge<V, F> {
	private final ValueBridge<V, F> bridge;
	private final IndexFieldAccessor<? super F> indexFieldAccessor;

	BoundValueBridge(ValueBridge<V, F> bridge, IndexFieldAccessor<? super F> indexFieldAccessor) {
		this.bridge = bridge;
		this.indexFieldAccessor = indexFieldAccessor;
	}

	public ValueBridge<V, F> getBridge() {
		return bridge;
	}

	public IndexFieldAccessor<? super F> getIndexFieldAccessor() {
		return indexFieldAccessor;
	}
}
