/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.List;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;

class HibernateOrmEntityTypeModel<T> extends AbstractHibernateOrmTypeModel<T> {

	private final EntityPersister persister;

	HibernateOrmEntityTypeModel(HibernateOrmIntrospector introspector, Class<T> clazz, EntityPersister persister) {
		super( introspector, clazz );
		this.persister = persister;
	}

	@Override
	PojoPropertyModel<?> createPropertyModel(String propertyName, List<XProperty> declaredXProperties) {
		EntityMetamodel metamodel = persister.getEntityMetamodel();
		Integer index = metamodel.getPropertyIndexOrNull( propertyName );

		if ( index != null ) {
			EntityTuplizer tuplizer = persister.getEntityTuplizer();
			Getter getter = tuplizer.getGetter( index );
			return new HibernateOrmPropertyModel<>(
					introspector, this, propertyName,
					declaredXProperties, getter
			);
		}
		else if ( propertyName.equals( metamodel.getIdentifierProperty().getName() ) ) {
			EntityTuplizer tuplizer = persister.getEntityTuplizer();
			Getter getter = tuplizer.getIdentifierGetter();
			return new HibernateOrmPropertyModel<>(
					introspector, this, propertyName,
					declaredXProperties, getter
			);
		}
		else {
			// The property is not part of the Hibernate ORM metamodel, probably because it's marked as @Transient
			return introspector.createFallbackPropertyModel(
					this,
					// FIXME: try to take the entity's default access type into account even in this case
					null,
					persister.getEntityMode(),
					propertyName,
					declaredXProperties
			);
		}
	}
}
