/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.AbstractPojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public abstract class AbstractPojoHCAnnRawTypeModel<T, I extends AbstractPojoHCAnnBootstrapIntrospector>
		extends AbstractPojoRawTypeModel<T, I> {

	protected final XClass xClass;
	final RawTypeDeclaringContext<T> rawTypeDeclaringContext;

	private Map<String, XProperty> declaredFieldAccessXPropertiesByName;
	private Map<String, List<XProperty>> declaredMethodAccessXPropertiesByName;

	public AbstractPojoHCAnnRawTypeModel(I introspector, PojoRawTypeIdentifier<T> typeIdentifier,
			RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		super( introspector, typeIdentifier );
		this.xClass = introspector.toXClass( typeIdentifier.javaClass() );
		this.rawTypeDeclaringContext = rawTypeDeclaringContext;
	}

	@Override
	public boolean isAbstract() {
		return xClass.isAbstract();
	}

	@Override
	public final boolean isSubTypeOf(MappableTypeModel other) {
		return other instanceof AbstractPojoHCAnnRawTypeModel
				&& ( (AbstractPojoHCAnnRawTypeModel<?, ?>) other ).xClass.isAssignableFrom( xClass );
	}

	@Override
	public Optional<PojoTypeModel<?>> typeArgument(Class<?> rawSuperType, int typeParameterIndex) {
		return rawTypeDeclaringContext.typeArgument( rawSuperType, typeParameterIndex );
	}

	@Override
	public Optional<PojoTypeModel<?>> arrayElementType() {
		return rawTypeDeclaringContext.arrayElementType();
	}

	@Override
	public Stream<Annotation> annotations() {
		return introspector.annotations( xClass );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<PojoConstructorModel<T>> createDeclaredConstructors() {
		return Arrays.stream( javaClass().getDeclaredConstructors() )
				.<PojoConstructorModel<T>>map( constructor -> new PojoHCAnnConstructorModel<>(
						introspector, this, (Constructor<T>) constructor ) )
				.collect( Collectors.toList() );
	}

	Class<T> javaClass() {
		return typeIdentifier.javaClass();
	}

	@Override
	protected final Stream<String> declaredPropertyNames() {
		return Stream.concat(
				declaredFieldAccessXPropertiesByName().keySet().stream(),
				declaredMethodAccessXPropertiesByName().keySet().stream()
		)
				.distinct();
	}

	protected final Map<String, XProperty> declaredFieldAccessXPropertiesByName() {
		if ( declaredFieldAccessXPropertiesByName == null ) {
			declaredFieldAccessXPropertiesByName =
					introspector.declaredFieldAccessXPropertiesByName( xClass );
		}
		return declaredFieldAccessXPropertiesByName;
	}

	protected final Map<String, List<XProperty>> declaredMethodAccessXPropertiesByName() {
		if ( declaredMethodAccessXPropertiesByName == null ) {
			declaredMethodAccessXPropertiesByName =
					introspector.declaredMethodAccessXPropertiesByName( xClass );
		}
		return declaredMethodAccessXPropertiesByName;
	}

	protected final List<Member> declaredPropertyGetters(String propertyName) {
		List<XProperty> methodAccessXProperties = declaredMethodAccessXPropertiesByName().get( propertyName );
		if ( methodAccessXProperties != null ) {
			return methodAccessXProperties.stream().map( PojoCommonsAnnotationsHelper::extractUnderlyingMember )
					.collect( Collectors.toList() );
		}
		return null;
	}

	protected final Member declaredPropertyField(String propertyName) {
		XProperty fieldAccessXProperty = declaredFieldAccessXPropertiesByName().get( propertyName );
		if ( fieldAccessXProperty != null ) {
			return PojoCommonsAnnotationsHelper.extractUnderlyingMember( fieldAccessXProperty );
		}
		return null;
	}

}
