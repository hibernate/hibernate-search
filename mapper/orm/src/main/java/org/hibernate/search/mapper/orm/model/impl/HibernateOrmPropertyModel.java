/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class HibernateOrmPropertyModel<T> implements PojoPropertyModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmBootstrapIntrospector introspector;
	private final HibernateOrmRawTypeModel<?> holderTypeModel;

	private final String name;
	/**
	 * The declared XProperties for this property in the holder type.
	 * May be empty if this property is declared in a supertype of the holder type
	 * and not overridden in the holder type.
 	 */
	private final List<XProperty> declaredXProperties;
	private final HibernateOrmBasicPropertyMetadata ormPropertyMetadata;

	private final Member member;

	private ValueReadHandle<T> handle;
	private PojoGenericTypeModel<T> typeModel;

	HibernateOrmPropertyModel(HibernateOrmBootstrapIntrospector introspector, HibernateOrmRawTypeModel<?> holderTypeModel,
			String name, List<XProperty> declaredXProperties,
			HibernateOrmBasicPropertyMetadata ormPropertyMetadata,
			Member member) {
		this.introspector = introspector;
		this.holderTypeModel = holderTypeModel;
		this.name = name;
		this.declaredXProperties = declaredXProperties;
		this.ormPropertyMetadata = ormPropertyMetadata;
		this.member = member;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Stream<Annotation> getAnnotations() {
		return declaredXProperties.stream().flatMap( introspector::getAnnotations );
	}

	@Override
	/*
	 * The cast is safe as long as both type parameter T and getGetterGenericReturnType
	 * match the actual type for this property.
	 */
	@SuppressWarnings( "unchecked" )
	public PojoGenericTypeModel<T> getTypeModel() {
		if ( typeModel == null ) {
			try {
				typeModel = (PojoGenericTypeModel<T>) holderTypeModel.getRawTypeDeclaringContext()
						.createGenericTypeModel( getGetterGenericReturnType() );
			}
			catch (RuntimeException e) {
				throw log.errorRetrievingPropertyTypeModel( getName(), holderTypeModel, e );
			}
		}
		return typeModel;
	}

	@Override
	@SuppressWarnings("unchecked") // By construction, we know the member returns values of type T
	public ValueReadHandle<T> getHandle() {
		if ( handle == null ) {
			try {
				handle = (ValueReadHandle<T>) introspector.createValueReadHandle( member, ormPropertyMetadata );
			}
			catch (IllegalAccessException | RuntimeException e) {
				throw log.errorRetrievingPropertyTypeModel( getName(), holderTypeModel, e );
			}
		}
		return handle;
	}

	Type getGetterGenericReturnType() {
		// Try to preserve generics information if possible
		if ( member instanceof Method ) {
			return ( (Method) member ).getGenericReturnType();
		}
		else if ( member instanceof Field ) {
			return ( (Field) member ).getGenericType();
		}
		else {
			throw new AssertionFailure(
					"Unexpected type for a " + Member.class.getName() + ": " + member
					+ " has type " + ( member == null ? null : member.getClass() )
			);
		}
	}

	Member getMember() {
		return member;
	}
}
