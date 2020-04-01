/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.backend.document.IndexFilterReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;

public final class BoundFilterBridge<T> {
	private final BeanHolder<T> factoryHolder;
	private final IndexFilterReference<?> indexFilterReference;

	BoundFilterBridge(BeanHolder<T> bridgeHolder, IndexFilterReference<?> indexFilterReference) {
		this.factoryHolder = bridgeHolder;
		this.indexFilterReference = indexFilterReference;
	}

	public BeanHolder<T> getBridgeHolder() {
		return factoryHolder;
	}

	public IndexFilterReference<?> getIndexFilterReference() {
		return indexFilterReference;
	}
}
