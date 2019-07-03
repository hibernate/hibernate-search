/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

public final class BoundValueBridge<V, F> {
	private final BeanHolder<? extends ValueBridge<? super V, F>> bridgeHolder;
	private final IndexFieldReference<F> indexFieldReference;

	BoundValueBridge(BeanHolder<? extends ValueBridge<? super V, F>> bridgeHolder,
			IndexFieldReference<F> indexFieldReference) {
		this.bridgeHolder = bridgeHolder;
		this.indexFieldReference = indexFieldReference;
	}

	public BeanHolder<? extends ValueBridge<? super V, F>> getBridgeHolder() {
		return bridgeHolder;
	}

	public IndexFieldReference<F> getIndexFieldReference() {
		return indexFieldReference;
	}
}
