/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.loading.impl;

import java.util.Map;

import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReferenceFactoryDelegate;
import org.hibernate.search.mapper.pojo.loading.impl.PojoLoadingPlan;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;

public class PojoSearchLoadingContext<E> implements SearchLoadingContext<E> {
	private final Map<String, PojoSearchLoadingIndexedTypeContext<? extends E>> targetTypesByEntityName;
	private final PojoEntityReferenceFactoryDelegate entityReferenceFactoryDelegate;
	private final BridgeSessionContext sessionContext;
	private final PojoSelectionLoadingContext delegate;

	public PojoSearchLoadingContext(
			Map<String, PojoSearchLoadingIndexedTypeContext<? extends E>> targetTypesByEntityName,
			PojoEntityReferenceFactoryDelegate entityReferenceFactoryDelegate,
			BridgeSessionContext sessionContext,
			PojoSelectionLoadingContext delegate) {
		this.targetTypesByEntityName = targetTypesByEntityName;
		this.entityReferenceFactoryDelegate = entityReferenceFactoryDelegate;
		this.sessionContext = sessionContext;
		this.delegate = delegate;
	}

	@Override
	public PojoSelectionLoadingContext unwrap() {
		return delegate;
	}

	@Override
	public ProjectionHitMapper<E> createProjectionHitMapper() {
		PojoLoadingPlan<E> loadingPlan = PojoLoadingPlan.create( delegate, targetTypesByEntityName.values() );
		return new PojoProjectionHitMapper<>( targetTypesByEntityName, entityReferenceFactoryDelegate, sessionContext,
				loadingPlan );
	}
}
