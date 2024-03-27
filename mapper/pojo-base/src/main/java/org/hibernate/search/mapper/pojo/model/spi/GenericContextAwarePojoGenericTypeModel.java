/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;

/**
 * An implementation of {@link PojoTypeModel} that takes advantage of the context
 * in which a given property appears to derive more precise type information.
 * <p>
 * Instances wrap a {@link PojoRawTypeModel}, and propagate generics information to properties
 * and their type by wrapping the property models as well.
 * <p>
 * For instance, given the following model:
 * <pre><code>
 * class A&lt;T extends C&gt; {
 *   GenericType&lt;T&gt; propertyOfA;
 * }
 * class B extends A&lt;D&gt; {
 * }
 * class C {
 * }
 * class D extends C {
 * }
 * class GenericType&lt;T&gt; {
 *   T propertyOfGenericType;
 * }
 * </code></pre>
 *
 * ... if an instance of this implementation was used to model the type of {@code B.propertyOfA},
 * then the property {@code B.propertyOfA} would appear to have type {@code List<D>} as one would expect,
 * instead of type {@code T extends C} if we inferred the type solely based on generics information from type {@code A}.
 *
 * This will also be true for more deeply nested references to a type variable,
 * for instance the type of property {@code B.propertyOfA.propertyOfGenericType} will correctly be inferred as D.
 */
public final class GenericContextAwarePojoGenericTypeModel<T>
		extends AbstractPojoGenericTypeModel<T>
		implements PojoTypeModel<T> {

	private static Optional<PojoTypeModel<?>> typeArgument(Helper helper,
			GenericTypeContext selfTypeContext, Class<?> rawSuperType, int typeParameterIndex) {
		return selfTypeContext.resolveTypeArgument( rawSuperType, typeParameterIndex )
				.map( type -> new GenericContextAwarePojoGenericTypeModel<>( helper,
						new GenericTypeContext( selfTypeContext.declaringContext(), type ) ) );
	}

	private static Optional<PojoTypeModel<?>> arrayElementType(Helper helper,
			GenericTypeContext selfTypeContext) {
		return selfTypeContext.resolveArrayElementType()
				.map( type -> new GenericContextAwarePojoGenericTypeModel<>( helper,
						new GenericTypeContext( selfTypeContext.declaringContext(), type ) ) );
	}

	private final Helper helper;
	private final GenericTypeContext genericTypeContext;

	private final Map<Object, PojoPropertyModel<?>> genericPropertyCache = new HashMap<>();

	public interface Helper {
		<T> PojoRawTypeModel<T> rawTypeModel(Class<T> clazz);

		Object propertyCacheKey(PojoPropertyModel<?> rawPropertyModel);

		Type propertyGenericType(PojoPropertyModel<?> rawPropertyModel);
	}

	public static class RawTypeDeclaringContext<T> {

		private final Helper helper;

		private final GenericTypeContext genericTypeContext;

		public RawTypeDeclaringContext(Helper helper, Class<T> rawType) {
			this.helper = helper;
			this.genericTypeContext = new GenericTypeContext( rawType );
		}

		public Optional<PojoTypeModel<?>> typeArgument(Class<?> rawSuperType, int typeParameterIndex) {
			return GenericContextAwarePojoGenericTypeModel
					.typeArgument( helper, genericTypeContext, rawSuperType, typeParameterIndex );
		}

		public Optional<PojoTypeModel<?>> arrayElementType() {
			return GenericContextAwarePojoGenericTypeModel
					.arrayElementType( helper, genericTypeContext );
		}

		/**
		 * @param declaredType The type to create a generic type model for.
		 * @return A generic type model for {@code declaredType} in this context.
		 * The type parameter can safely be assumed to be exactly the type {@code declaredType}.
		 * For instance if {@code declaredType} is {@code String.class},
		 * the returned type model will be an instance of {@code PojoGenericTypeModel<String>}.
		 * If {@code declaredType} is {@code List<String>}, it will be {@code PojoGenericTypeModel<List<String>>},
		 * and so on.
		 */
		public PojoTypeModel<?> memberTypeReference(Type declaredType) {
			/*
			 * The declaring type, even raw, could extend parameterized types in which the given "type" was declared.
			 * Thus we need the declaring context even for property types on raw types,
			 * so that those types are resolved properly.
			 */
			return new GenericContextAwarePojoGenericTypeModel<>(
					helper, new GenericTypeContext( genericTypeContext, declaredType )
			);
		}
	}

	@SuppressWarnings("unchecked") // Can't do better here, this code is all about reflection
	private GenericContextAwarePojoGenericTypeModel(Helper helper, GenericTypeContext genericTypeContext) {
		super( helper.rawTypeModel( (Class<? super T>) genericTypeContext.rawType() ) );
		this.helper = helper;
		this.genericTypeContext = genericTypeContext;
	}

	@Override
	public String name() {
		return genericTypeContext.name();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		GenericContextAwarePojoGenericTypeModel<?> that = (GenericContextAwarePojoGenericTypeModel<?>) o;
		return Objects.equals( genericTypeContext, that.genericTypeContext );
	}

	@Override
	public int hashCode() {
		return Objects.hash( genericTypeContext );
	}

	@Override
	public PojoPropertyModel<?> property(String propertyName) {
		return wrapProperty( super.property( propertyName ) );
	}

	@Override
	public <U> Optional<PojoTypeModel<? extends U>> castTo(Class<U> target) {
		return Optional.of( new GenericContextAwarePojoGenericTypeModel<U>( helper,
				genericTypeContext.castTo( target ) ) );
	}

	@Override
	public Optional<PojoTypeModel<?>> typeArgument(Class<?> rawSuperType, int typeParameterIndex) {
		return typeArgument( helper, genericTypeContext, rawSuperType, typeParameterIndex );
	}

	@Override
	public Optional<PojoTypeModel<?>> arrayElementType() {
		return arrayElementType( helper, genericTypeContext );
	}

	private <U> PojoPropertyModel<? extends U> wrapProperty(PojoPropertyModel<U> rawPropertyModel) {
		Object cacheKey = helper.propertyCacheKey( rawPropertyModel );
		@SuppressWarnings("unchecked") // See how we add values to the cache
		PojoPropertyModel<? extends U> cached = (PojoPropertyModel<? extends U>) genericPropertyCache.get( cacheKey );
		if ( cached != null ) {
			return cached;
		}
		Type propertyType = helper.propertyGenericType( rawPropertyModel );
		GenericTypeContext propertyGenericTypeContext = new GenericTypeContext( genericTypeContext, propertyType );
		GenericContextAwarePojoGenericTypeModel<? extends U> genericPropertyTypeModel =
				new GenericContextAwarePojoGenericTypeModel<>( helper, propertyGenericTypeContext );
		PojoPropertyModel<? extends U> propertyModel =
				new GenericContextAwarePojoPropertyModel<>( rawPropertyModel, genericPropertyTypeModel );
		genericPropertyCache.put( cacheKey, propertyModel );
		return propertyModel;
	}

}
