/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

public final class BoundValueBridge<T, R> {
	private final ValueBridge<T, R> bridge;
	private final IndexFieldAccessor<? super R> indexFieldAccessor;

	BoundValueBridge(ValueBridge<T, R> bridge, IndexFieldAccessor<? super R> indexFieldAccessor) {
		this.bridge = bridge;
		this.indexFieldAccessor = indexFieldAccessor;
	}

	public ValueBridge<T, R> getBridge() {
		return bridge;
	}

	public IndexFieldAccessor<? super R> getIndexFieldAccessor() {
		return indexFieldAccessor;
	}
}
