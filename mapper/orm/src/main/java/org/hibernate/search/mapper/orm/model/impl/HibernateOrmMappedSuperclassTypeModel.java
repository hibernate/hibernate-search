/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.reflect.Member;
import java.util.List;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.EntityMode;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

class HibernateOrmMappedSuperclassTypeModel<T> extends AbstractHibernateOrmTypeModel<T> {

	private final ManagedType<T> managedType;

	HibernateOrmMappedSuperclassTypeModel(HibernateOrmBootstrapIntrospector introspector, ManagedType<T> managedType,
			RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		super( introspector, managedType.getJavaType(), rawTypeDeclaringContext );
		this.managedType = managedType;
	}

	@Override
	PojoPropertyModel<?> createPropertyModel(String propertyName, List<XProperty> declaredXProperties) {
		Attribute<? super T, ?> attribute = managedType.getAttribute( propertyName );
		if ( attribute != null ) {
			Member member = attribute.getJavaMember();
			return introspector.createMemberPropertyModel(
					this,
					propertyName,
					member,
					declaredXProperties
			);
		}
		else {
			// The property is not part of the Hibernate ORM metamodel, probably because it's marked as @Transient
			return introspector.createFallbackPropertyModel(
					this,
					// FIXME: try to take the mappedSuperclass's default access type into account even in this case
					null,
					EntityMode.POJO,
					propertyName,
					declaredXProperties
			);
		}
	}
}
