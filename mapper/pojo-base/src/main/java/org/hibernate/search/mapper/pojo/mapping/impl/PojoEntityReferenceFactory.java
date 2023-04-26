/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
