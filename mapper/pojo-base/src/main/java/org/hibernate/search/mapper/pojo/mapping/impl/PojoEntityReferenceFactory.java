/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReferenceFactoryDelegate;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkTypeContext;

final class PojoEntityReferenceFactory implements EntityReferenceFactory {

	public final PojoEntityReferenceFactoryDelegate delegate;
	private final PojoTypeManagerContainer typeManagers;

	PojoEntityReferenceFactory(PojoEntityReferenceFactoryDelegate delegate,
			PojoTypeManagerContainer typeManagers) {
		this.delegate = delegate;
		this.typeManagers = typeManagers;
	}

	@Override
	public EntityReference createEntityReference(String typeName, Object identifier) {
		PojoWorkTypeContext<?, ?> typeContext = typeManagers.byEntityName().getOrFail( typeName );
		return delegate.create( typeContext.typeIdentifier(), typeContext.entityName(), identifier );
	}
}
