/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.model.spi.TypeModel;


/**
 * An implementation of {@link PojoGenericTypeModel} that will erase generics information
 * before returning models of type arguments.
 * <p>
 * For instance, given the following model:
 * <pre><code>
 * class A {
 *   List&lt;B&lt;D&gt;&gt; propertyOfA;
 * }
 * class B&lt;T extends C&gt; {
 *   T propertyOfB;
 * }
 * class C {
 * }
 * class D extends C {
 * }
 * </code></pre>
 * ... an instance of this implementation modeling the type of {@code propertyOfA}
 * would return a model of type {@code B} instead of a model of type {@code B<D>}
 * when calling {@link #getTypeArgument(Class, int) getTypeArgument(List.class, 0)}.
 * As a result, an IndexedEmbedded on {@code propertyOfB} nested in another IndexedEmbedded on {@code propertyOfA}
 * would embed the properties of type {@code C} (the upper bound of type parameter {@code T}),
 * instead of embedding the properties of type {@code D} as one would expect.
 * <p>
 * This behavior is clearly not ideal, but it's by far the easiest way to implement {@link ErasingPojoGenericTypeModel},
 * because it allows to only implement {@link PojoTypeModel} for raw types.
 * One could imagine going one step further and retain generic information from the holding type
 * when inspecting the type of a property, but this would require some additional work,
 * both in the engine and in the type model implementations.
 */
public final class ErasingPojoGenericTypeModel<T> implements PojoGenericTypeModel<T> {

	private final PojoIntrospector introspector;
	private final PojoRawTypeModel<? super T> rawTypeModel;
	private final Type type;

	public ErasingPojoGenericTypeModel(PojoIntrospector introspector, PojoRawTypeModel<? super T> rawTypeModel,
			Type type) {
		this.introspector = introspector;
		this.rawTypeModel = rawTypeModel;
		this.type = type;
	}

	@Override
	public boolean isSubTypeOf(TypeModel other) {
		return rawTypeModel.isSubTypeOf( other );
	}

	@Override
	public Stream<? extends PojoRawTypeModel<? super T>> getAscendingSuperTypes() {
		return rawTypeModel.getAscendingSuperTypes();
	}

	@Override
	public Stream<? extends PojoRawTypeModel<? super T>> getDescendingSuperTypes() {
		return rawTypeModel.getDescendingSuperTypes();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + type.getTypeName() + "]";
	}

	@Override
	public PojoRawTypeModel<? super T> getRawType() {
		return rawTypeModel;
	}

	@Override
	public <U> Optional<PojoTypeModel<U>> getSuperType(Class<U> superClassCandidate) {
		return rawTypeModel.getSuperType( superClassCandidate );
	}

	@Override
	public <A extends Annotation> Optional<A> getAnnotationByType(Class<A> annotationType) {
		return rawTypeModel.getAnnotationByType( annotationType );
	}

	@Override
	public <A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType) {
		return rawTypeModel.getAnnotationsByType( annotationType );
	}

	@Override
	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(
			Class<? extends Annotation> metaAnnotationType) {
		return rawTypeModel.getAnnotationsByMetaAnnotationType( metaAnnotationType );
	}

	@Override
	public PojoPropertyModel<?> getProperty(String propertyName) {
		return rawTypeModel.getProperty( propertyName );
	}

	@Override
	public Stream<PojoPropertyModel<?>> getDeclaredProperties() {
		return rawTypeModel.getDeclaredProperties();
	}

	@Override
	@SuppressWarnings("unchecked") // We cannot perform runtime checks of generics on an instance
	public T cast(Object instance) {
		return (T) rawTypeModel.cast( instance );
	}

	@Override
	public Optional<PojoGenericTypeModel<?>> getTypeArgument(Class<?> rawSuperType, int typeParameterIndex) {
		Class<?> rawTypeArgument = null;
		/*
		 * FIXME this is wrong, the type may not be parameterized but still implement the raw superclass,
		 * or it could parameterized but with totally unrelated parameters, for instance:
		 *
		 * class A<T> {
		 * }
		 * class B<U> extends A<String> {
		 * }
		 *
		 * B definitely implement A, but T is definitely not the same as U.
		 *
		 * FIXME also, the type argument may not be a Class<?>, in which case we need to convert it.
		 */
		if ( type instanceof ParameterizedType ) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Class<?> rawType = (Class<?>) parameterizedType.getRawType();
			if ( rawSuperType.isAssignableFrom( rawType ) ) {
				Type[] typeArguments = parameterizedType.getActualTypeArguments();
				rawTypeArgument = (Class<?>) typeArguments[typeParameterIndex];
			}
		}
		if ( rawTypeArgument != null ) {
			return Optional.of( new ErasingPojoGenericTypeModel<>(
					introspector,
					introspector.getTypeModel( rawTypeArgument ),
					rawTypeArgument
			) );
		}
		else {
			return Optional.empty();
		}
	}

}
