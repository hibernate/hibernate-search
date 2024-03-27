/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;

public final class BoundIdentifierBridge<I> {
	private final BeanHolder<? extends IdentifierBridge<I>> bridgeHolder;

	BoundIdentifierBridge(BeanHolder<? extends IdentifierBridge<I>> bridgeHolder) {
		this.bridgeHolder = bridgeHolder;
	}

	public BeanHolder<? extends IdentifierBridge<I>> getBridgeHolder() {
		return bridgeHolder;
	}
}
