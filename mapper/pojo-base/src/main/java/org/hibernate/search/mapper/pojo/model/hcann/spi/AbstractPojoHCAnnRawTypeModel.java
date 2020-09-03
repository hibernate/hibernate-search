/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.AbstractPojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public abstract class AbstractPojoHCAnnRawTypeModel<T, I extends AbstractPojoHCAnnBootstrapIntrospector>
		extends AbstractPojoRawTypeModel<T, I> {

	protected final XClass xClass;
	final RawTypeDeclaringContext<T> rawTypeDeclaringContext;

	private Map<String, XProperty> declaredFieldAccessXPropertiesByName;
	private Map<String, XProperty> declaredMethodAccessXPropertiesByName;

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
	public Stream<Annotation> annotations() {
		return introspector.annotations( xClass );
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

	protected final Map<String, XProperty> declaredMethodAccessXPropertiesByName() {
		if ( declaredMethodAccessXPropertiesByName == null ) {
			declaredMethodAccessXPropertiesByName =
					introspector.declaredMethodAccessXPropertiesByName( xClass );
		}
		return declaredMethodAccessXPropertiesByName;
	}

	protected final Member declaredPropertyGetter(String propertyName) {
		XProperty methodAccessXProperty = declaredMethodAccessXPropertiesByName().get( propertyName );
		if ( methodAccessXProperty != null ) {
			// Method access is available. Get values from the getter.
			return PojoCommonsAnnotationsHelper.extractUnderlyingMember( methodAccessXProperty );
		}
		return null;
	}

	protected final Member declaredPropertyField(String propertyName) {
		XProperty fieldAccessXProperty = declaredFieldAccessXPropertiesByName().get( propertyName );
		if ( fieldAccessXProperty != null ) {
			// Method access is available. Get values from the getter.
			return PojoCommonsAnnotationsHelper.extractUnderlyingMember( fieldAccessXProperty );
		}
		return null;
	}

}
