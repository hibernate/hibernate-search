/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;

class EntityTypeModel<T> implements TypeModel<T> {

	private final HibernateOrmIntrospector introspector;
	private final Class<T> type;
	private final EntityPersister persister;

	EntityTypeModel(
			HibernateOrmIntrospector introspector,
			Class<T> type,
			EntityPersister persister) {
		this.introspector = introspector;
		this.type = type;
		this.persister = persister;
	}

	@Override
	public Class<T> getJavaType() {
		return type;
	}

	@Override
	public PropertyModel<?> getProperty(String propertyName) {
		EntityMetamodel metamodel = persister.getEntityMetamodel();
		Integer index = metamodel.getPropertyIndexOrNull( propertyName );
		if ( index != null ) {
			EntityTuplizer tuplizer = persister.getEntityTuplizer();
			Getter getter = tuplizer.getGetter( index );
			return new HibernateOrmPropertyModel<>( introspector, this, propertyName, getter );
		}
		else {
			// The property is not part of the Hibernate ORM metamodel, probably because it's marked as @Transient
			return introspector.createFallbackPropertyModel(
					this,
					// FIXME: try to take the entity's default access type into account even in this case
					null,
					persister.getEntityMode(),
					propertyName
			);
		}
	}
}
