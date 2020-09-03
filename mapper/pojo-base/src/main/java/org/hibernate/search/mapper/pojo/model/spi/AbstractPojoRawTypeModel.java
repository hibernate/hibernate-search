/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractPojoRawTypeModel<T, I extends PojoBootstrapIntrospector> implements PojoRawTypeModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final I introspector;
	protected final PojoRawTypeIdentifier<T> typeIdentifier;
	private final PojoCaster<T> caster;

	private final Map<String, PojoPropertyModel<?>> propertyModelCache = new HashMap<>();

	private List<PojoPropertyModel<?>> declaredProperties;

	public AbstractPojoRawTypeModel(I introspector, PojoRawTypeIdentifier<T> typeIdentifier) {
		this.introspector = introspector;
		this.typeIdentifier = typeIdentifier;
		this.caster = new JavaClassPojoCaster<>( typeIdentifier.javaClass() );
	}

	@Override
	public final boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		AbstractPojoRawTypeModel<?, ?> that = (AbstractPojoRawTypeModel<?, ?>) o;
		/*
		 * We need to take the introspector into account, so that the engine does not confuse
		 * type models from different mappers during bootstrap.
		 */
		return Objects.equals( introspector, that.introspector ) &&
				Objects.equals( typeIdentifier, that.typeIdentifier );
	}

	@Override
	public final int hashCode() {
		return Objects.hash( introspector, typeIdentifier );
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "[" + typeIdentifier + "]";
	}

	@Override
	public final PojoRawTypeIdentifier<T> typeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public final String name() {
		return typeIdentifier.toString();
	}

	@Override
	public final PojoPropertyModel<?> property(String propertyName) {
		PojoPropertyModel<?> propertyModel = propertyOrNull( propertyName );
		if ( propertyModel == null ) {
			throw log.cannotFindReadableProperty( this, propertyName );
		}
		return propertyModel;
	}

	@Override
	public final Stream<PojoPropertyModel<?>> declaredProperties() {
		if ( declaredProperties == null ) {
			declaredProperties = declaredPropertyNames()
					.map( this::propertyOrNull )
					.filter( Objects::nonNull )
					.collect( Collectors.toList() );
		}
		return declaredProperties.stream();
	}

	@Override
	public final PojoCaster<T> caster() {
		return caster;
	}

	protected abstract Stream<String> declaredPropertyNames();

	protected abstract PojoPropertyModel<?> createPropertyModel(String propertyName);

	private PojoPropertyModel<?> propertyOrNull(String propertyName) {
		return propertyModelCache.computeIfAbsent( propertyName, this::createPropertyModel );
	}

}
