/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

public final class BoundValueBridge<V, F> {
	private final BeanHolder<? extends ValueBridge<? super V, F>> bridgeHolder;
	private final IndexFieldAccessor<? super F> indexFieldAccessor;

	BoundValueBridge(BeanHolder<? extends ValueBridge<? super V, F>> bridgeHolder,
			IndexFieldAccessor<? super F> indexFieldAccessor) {
		this.bridgeHolder = bridgeHolder;
		this.indexFieldAccessor = indexFieldAccessor;
	}

	public BeanHolder<? extends ValueBridge<? super V, F>> getBridgeHolder() {
		return bridgeHolder;
	}

	public IndexFieldAccessor<? super F> getIndexFieldAccessor() {
		return indexFieldAccessor;
	}
}
