/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoContainerTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.spi.LoggerFactory;

class HibernateOrmPropertyModel<T> implements PojoPropertyModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmIntrospector introspector;
	private final PojoTypeModel<?> holderTypeModel;

	private final String name;
	private final Getter getter;
	private final List<XProperty> declaredXProperties;

	private PropertyHandle handle;
	private XProperty getterXProperty;
	private PojoTypeModel<T> typeModel;
	private Optional<PojoContainerTypeModel<?>> containerTypeModelOptional;

	HibernateOrmPropertyModel(HibernateOrmIntrospector introspector, PojoTypeModel<?> holderTypeModel,
			String name, List<XProperty> declaredXProperties, Getter getter) {
		this.introspector = introspector;
		this.holderTypeModel = holderTypeModel;
		this.name = name;
		this.getter = getter;
		this.declaredXProperties = declaredXProperties;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getJavaClass() {
		return getter.getReturnType();
	}

	@Override
	public <A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType) {
		return declaredXProperties.stream().flatMap(
				xProperty -> introspector.getAnnotationsByType( xProperty, annotationType )
		);
	}

	@Override
	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType) {
		return declaredXProperties.stream().flatMap(
				xProperty -> introspector.getAnnotationsByMetaAnnotationType( xProperty, metaAnnotationType )
		);
	}

	@Override
	public PojoTypeModel<T> getTypeModel() {
		if ( typeModel == null ) {
			try {
				typeModel = introspector.getTypeModel( getJavaClass() );
			}
			catch (RuntimeException e) {
				throw new SearchException( "Exception while retrieving property type model for '"
						+ getName() + "' on '" + holderTypeModel + "'", e );
			}
		}
		return typeModel;
	}

	@Override
	public Optional<PojoContainerTypeModel<?>> getContainerTypeModel() {
		if ( containerTypeModelOptional == null ) {
			/*
			 * Make sure to retrieve the type from the XProperty pointing to the same property as the getter,
			 * which could be declared in a supertype because of the JPA access type.
			 */
			XProperty getterXProperty = getGetterXProperty();
			if ( getterXProperty == null ) {
				// Something is probably wrong...
				containerTypeModelOptional = Optional.empty();
			}
			else if ( getterXProperty.isCollection() || getterXProperty.isArray() ) {
				XClass containerXClass = getterXProperty.getType();
				XClass elementXClass = getterXProperty.getElementClass();
				containerTypeModelOptional = Optional.of( new HibernateOrmContainerTypeModel<>(
						introspector.toClass( containerXClass ),
						introspector.getTypeModel( elementXClass )
				) );
			}
			else {
				containerTypeModelOptional = Optional.empty();
			}
		}
		return containerTypeModelOptional;
	}

	@Override
	public PropertyHandle getHandle() {
		if ( handle == null ) {
			handle = new GetterPropertyHandle( name, getter );
		}
		return handle;
	}

	private XProperty getGetterXProperty() {
		if ( getterXProperty == null ) {
			Method method = getter.getMethod();
			Member member = getter.getMember();
			if ( method != null ) {
				this.getterXProperty = introspector.toXProperty( method, name ).orElse( null );
			}
			else if ( member != null ) {
				this.getterXProperty = introspector.toXProperty( member, name ).orElse( null );
			}
			if ( getterXProperty == null ) {
				throw log.unknownPropertyForGetter( typeModel.getJavaClass(), name, getter );
			}
		}

		return getterXProperty;
	}
}
