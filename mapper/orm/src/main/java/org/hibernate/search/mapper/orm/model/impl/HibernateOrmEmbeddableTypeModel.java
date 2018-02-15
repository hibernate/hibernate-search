/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.List;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.metamodel.internal.EmbeddableTypeImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.tuple.component.ComponentTuplizer;
import org.hibernate.type.ComponentType;

class HibernateOrmEmbeddableTypeModel<T> extends AbstractHibernateOrmTypeModel<T> {

	private final ComponentType componentType;

	HibernateOrmEmbeddableTypeModel(HibernateOrmIntrospector introspector, EmbeddableType<T> embeddableType) {
		super( introspector, embeddableType.getJavaType() );
		// FIXME find a way to avoid depending on Hibernate ORM internal APIs
		EmbeddableTypeImpl<T> embeddableTypeImpl = (EmbeddableTypeImpl<T>) embeddableType;
		this.componentType = embeddableTypeImpl.getHibernateType();
	}

	@Override
	PojoPropertyModel<?> createPropertyModel(String propertyName, List<XProperty> xProperties) {
		Integer index = getPropertyIndexOrNull( componentType, propertyName );
		if ( index != null ) {
			ComponentTuplizer tuplizer = componentType.getComponentTuplizer();
			Getter getter = tuplizer.getGetter( index );
			return new HibernateOrmPropertyModel<>(
					introspector, this, propertyName, xProperties, getter
			);
		}
		else {
			// The property is not part of the Hibernate ORM metamodel, probably because it's marked as @Transient
			return introspector.createFallbackPropertyModel(
					this,
					// FIXME: try to take the embeddable's default access type into account even in this case
					null,
					componentType.getEntityMode(),
					propertyName,
					xProperties
			);
		}
	}

	private static Integer getPropertyIndexOrNull(ComponentType componentType, String propertyName) {
		String[] names = componentType.getPropertyNames();
		for ( int i = 0, max = names.length; i < max; i++ ) {
			if ( names[i].equals( propertyName ) ) {
				return i;
			}
		}
		return null;
	}
}
