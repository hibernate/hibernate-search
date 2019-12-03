/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContext;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeTypeContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmScopeTypeContext<E>, HibernateOrmListenerTypeContext,
				HibernateOrmSessionTypeContext<E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final String jpaEntityName;
	private final EntityPersister entityPersister;
	private final EntityTypeDescriptor<E> entityTypeDescriptor;

	AbstractHibernateOrmTypeContext(SessionFactoryImplementor sessionFactory,
			PojoRawTypeIdentifier<E> typeIdentifier, String jpaEntityName, String hibernateOrmEntityName) {
		this.typeIdentifier = typeIdentifier;
		this.jpaEntityName = jpaEntityName;
		MetamodelImplementor metamodel = sessionFactory.getMetamodel();
		this.entityPersister = metamodel.entityPersister( hibernateOrmEntityName );
		this.entityTypeDescriptor = metamodel.entity( entityPersister.getEntityName() );
	}

	@Override
	public PojoRawTypeIdentifier<E> getTypeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public String getJpaEntityName() {
		return jpaEntityName;
	}

	public String getHibernateOrmEntityName() {
		return entityPersister.getEntityName();
	}

	public EntityPersister getEntityPersister() {
		return entityPersister;
	}

	public EntityTypeDescriptor<E> getEntityTypeDescriptor() {
		if ( entityTypeDescriptor == null ) {
			// TODO HSEARCH-3771 Mass indexing for ORM's dynamic-map entity types
			// TODO HSEARCH-3772 Allow mapping the document id to non-entity-id properties for ORM's dynamic-map entity types
			throw log.nonJpaEntityType( typeIdentifier );
		}
		return entityTypeDescriptor;
	}
}
