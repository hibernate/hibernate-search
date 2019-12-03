/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.JavaClassPojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractHibernateOrmRawTypeModel<T> implements PojoRawTypeModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final HibernateOrmBootstrapIntrospector introspector;
	protected final PojoRawTypeIdentifier<T> typeIdentifier;
	protected final XClass xClass;
	private final PojoCaster<T> caster;

	private List<PojoPropertyModel<?>> declaredProperties;

	AbstractHibernateOrmRawTypeModel(HibernateOrmBootstrapIntrospector introspector,
			PojoRawTypeIdentifier<T> typeIdentifier) {
		this.introspector = introspector;
		this.typeIdentifier = typeIdentifier;
		this.xClass = introspector.toXClass( typeIdentifier.getJavaClass() );
		this.caster = new JavaClassPojoCaster<>( typeIdentifier.getJavaClass() );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		AbstractHibernateOrmRawTypeModel<?> that = (AbstractHibernateOrmRawTypeModel<?>) o;
		/*
		 * We need to take the introspector into account, so that the engine does not confuse
		 * type models from different mappers during bootstrap.
		 */
		return Objects.equals( introspector, that.introspector ) &&
				Objects.equals( typeIdentifier, that.typeIdentifier );
	}

	@Override
	public int hashCode() {
		return Objects.hash( introspector, typeIdentifier );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + typeIdentifier + "]";
	}

	@Override
	public PojoRawTypeIdentifier<T> getTypeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public final String getName() {
		return typeIdentifier.toString();
	}

	@Override
	public boolean isSubTypeOf(MappableTypeModel other) {
		return other instanceof AbstractHibernateOrmRawTypeModel
				&& ( (AbstractHibernateOrmRawTypeModel<?>) other ).xClass.isAssignableFrom( xClass );
	}

	@Override
	public final PojoRawTypeModel<? super T> getRawType() {
		return this;
	}

	@Override
	public abstract Stream<? extends AbstractHibernateOrmRawTypeModel<? super T>> getAscendingSuperTypes();

	@Override
	public abstract Stream<? extends AbstractHibernateOrmRawTypeModel<? super T>> getDescendingSuperTypes();

	@Override
	public final PojoPropertyModel<?> getProperty(String propertyName) {
		PojoPropertyModel<?> propertyModel = getPropertyOrNull( propertyName );
		if ( propertyModel == null ) {
			throw log.cannotFindReadableProperty( this, propertyName );
		}
		return propertyModel;
	}

	@Override
	public final Stream<PojoPropertyModel<?>> getDeclaredProperties() {
		if ( declaredProperties == null ) {
			// TODO HSEARCH-3056 remove lambdas if possible
			declaredProperties = getDeclaredPropertyNames()
					.map( this::getPropertyOrNull )
					.filter( Objects::nonNull )
					.collect( Collectors.toList() );
		}
		return declaredProperties.stream();
	}

	@Override
	public final PojoCaster<T> getCaster() {
		return caster;
	}

	abstract Stream<String> getDeclaredPropertyNames();

	abstract PojoPropertyModel<?> getPropertyOrNull(String propertyName);

}
