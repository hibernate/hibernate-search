/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.metamodel.internal.EmbeddableTypeImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.tuple.component.ComponentTuplizer;
import org.hibernate.type.ComponentType;

class EmbeddableTypeModel<T> implements TypeModel<T> {

	private final HibernateOrmIntrospector introspector;
	private final Class<T> type;
	private final ComponentType componentType;

	EmbeddableTypeModel(
			HibernateOrmIntrospector introspector,
			EmbeddableType<T> embeddableType) {
		this.introspector = introspector;
		this.type = embeddableType.getJavaType();
		// FIXME find a way to avoid depending on Hibernate ORM internal APIs
		EmbeddableTypeImpl<T> embeddableTypeImpl = (EmbeddableTypeImpl<T>) embeddableType;
		this.componentType = embeddableTypeImpl.getHibernateType();
	}

	@Override
	public Class<T> getJavaType() {
		return type;
	}

	@Override
	public PropertyModel<?> getProperty(String propertyName) {
		Integer index = getPropertyIndexOrNull( componentType, propertyName );
		if ( index != null ) {
			ComponentTuplizer tuplizer = componentType.getComponentTuplizer();
			Getter getter = tuplizer.getGetter( index );
			return new HibernateOrmPropertyModel<>( introspector, this, propertyName, getter );
		}
		else {
			// The property is not part of the Hibernate ORM metamodel, probably because it's marked as @Transient
			return introspector.createFallbackPropertyModel(
					this,
					// FIXME: try to take the embeddable's default access type into account even in this case
					null,
					componentType.getEntityMode(),
					propertyName
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
