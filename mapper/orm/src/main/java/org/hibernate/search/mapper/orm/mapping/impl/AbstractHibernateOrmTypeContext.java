/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

abstract class AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmListenerTypeContext, HibernateOrmSessionTypeContext<E> {

	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final String jpaEntityName;
	private final EntityPersister entityPersister;
	private final PojoPathFilter dirtyFilter;

	AbstractHibernateOrmTypeContext(AbstractBuilder<E> builder, SessionFactoryImplementor sessionFactory) {
		this.typeIdentifier = builder.typeIdentifier;
		this.jpaEntityName = builder.jpaEntityName;
		MetamodelImplementor metamodel = sessionFactory.getMetamodel();
		this.entityPersister = metamodel.entityPersister( builder.hibernateOrmEntityName );
		this.dirtyFilter = builder.dirtyFilter;
	}

	@Override
	public String toString() {
		return typeIdentifier().toString();
	}

	@Override
	public PojoRawTypeIdentifier<E> typeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public String jpaEntityName() {
		return jpaEntityName;
	}

	public String hibernateOrmEntityName() {
		return entityPersister.getEntityName();
	}

	public EntityPersister entityPersister() {
		return entityPersister;
	}

	@Override
	public PojoPathFilter dirtyFilter() {
		return dirtyFilter;
	}

	abstract static class AbstractBuilder<E> implements PojoTypeExtendedMappingCollector {
		private final PojoRawTypeIdentifier<E> typeIdentifier;
		private final String jpaEntityName;
		private final String hibernateOrmEntityName;
		private PojoPathFilter dirtyFilter;

		AbstractBuilder(PojoRawTypeIdentifier<E> typeIdentifier, String jpaEntityName, String hibernateOrmEntityName) {
			this.typeIdentifier = typeIdentifier;
			this.jpaEntityName = jpaEntityName;
			this.hibernateOrmEntityName = hibernateOrmEntityName;
		}

		@Override
		public void dirtyFilter(PojoPathFilter dirtyFilter) {
			this.dirtyFilter = dirtyFilter;
		}
	}
}
