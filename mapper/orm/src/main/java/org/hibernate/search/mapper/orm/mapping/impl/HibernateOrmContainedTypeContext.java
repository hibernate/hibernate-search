/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContextProvider;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoContainedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class HibernateOrmContainedTypeContext<E> extends AbstractHibernateOrmTypeContext<E> {

	private HibernateOrmContainedTypeContext(HibernateOrmContainedTypeContext.Builder<E> builder,
			PojoLoadingTypeContext<E> delegate, SessionFactoryImplementor sessionFactory) {
		super( builder, delegate, sessionFactory );
	}

	@Override
	public Object toIndexingPlanProvidedId(Object entityId) {
		// The concept of document ID is not relevant for contained types,
		// so we always provide the entity ID to indexing plans
		return entityId;
	}

	static class Builder<E> extends AbstractHibernateOrmTypeContext.Builder<E>
			implements PojoContainedTypeExtendedMappingCollector {

		Builder(PojoRawTypeModel<E> typeModel, PersistentClass persistentClass) {
			super( typeModel, persistentClass );
		}

		HibernateOrmContainedTypeContext<E> build(PojoLoadingTypeContextProvider delegateProvider,
				SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmContainedTypeContext<>( this, delegateProvider.forExactType( typeIdentifier ),
					sessionFactory );
		}
	}
}
