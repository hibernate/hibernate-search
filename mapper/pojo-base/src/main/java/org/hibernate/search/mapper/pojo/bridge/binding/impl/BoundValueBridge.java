/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	public ValueBridge<? super V, F> getBridge() {
		return bridgeHolder.get();
	}

	public IndexFieldReference<F> getIndexFieldReference() {
		return indexFieldReference;
	}
}
