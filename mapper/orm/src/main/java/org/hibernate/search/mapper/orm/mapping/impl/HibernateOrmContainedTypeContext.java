/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoContainedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class HibernateOrmContainedTypeContext<E> extends AbstractHibernateOrmTypeContext<E> {

	private HibernateOrmContainedTypeContext(HibernateOrmContainedTypeContext.Builder<E> builder,
			SessionFactoryImplementor sessionFactory) {
		super( builder, sessionFactory );
	}

	@Override
	public Object toIndexingPlanProvidedId(Object entityId) {
		// The concept of document ID is not relevant for contained types,
		// so we always provide the entity ID to indexing plans
		return entityId;
	}

	static class Builder<E> extends AbstractBuilder<E> implements PojoContainedTypeExtendedMappingCollector {

		Builder(PojoRawTypeModel<E> typeModel, String jpaEntityName, String hibernateOrmEntityName) {
			super( typeModel, jpaEntityName, hibernateOrmEntityName );
		}

		HibernateOrmContainedTypeContext<E> build(SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmContainedTypeContext<>( this, sessionFactory );
		}
	}
}
