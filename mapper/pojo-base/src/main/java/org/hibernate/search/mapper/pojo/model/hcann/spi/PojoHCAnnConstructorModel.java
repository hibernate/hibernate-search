/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;

public class PojoHCAnnConstructorModel<T> implements PojoConstructorModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractPojoHCAnnBootstrapIntrospector introspector;
	final AbstractPojoHCAnnRawTypeModel<T, ?> declaringTypeModel;
	private final Constructor<T> constructor;

	private List<PojoMethodParameterModel<?>> declaredParameters;
	private ValueCreateHandle<T> handleCache;

	public PojoHCAnnConstructorModel(AbstractPojoHCAnnBootstrapIntrospector introspector,
			AbstractPojoHCAnnRawTypeModel<T, ?> declaringTypeModel, Constructor<T> constructor) {
		this.introspector = introspector;
		this.declaringTypeModel = declaringTypeModel;
		this.constructor = constructor;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + declaringTypeModel.name() + "("
				+ CommaSeparatedClassesFormatter.format( parametersJavaTypes() ) + ")" + "]";
	}

	@Override
	public Stream<Annotation> annotations() {
		return Arrays.stream( constructor.getAnnotations() );
	}

	@Override
	public PojoRawTypeModel<T> typeModel() {
		return declaringTypeModel;
	}

	@Override
	public ValueCreateHandle<T> handle() {
		if ( handleCache == null ) {
			try {
				handleCache = introspector.createValueCreateHandle( constructor );
			}
			catch (ReflectiveOperationException | RuntimeException e) {
				throw log.errorRetrievingConstructorHandle( constructor, declaringTypeModel, e );
			}
		}
		return handleCache;
	}

	@Override
	public PojoMethodParameterModel<?> parameter(int index) {
		if ( index < 0 ) {
			throw log.cannotFindConstructorParameter( this, index );
		}
		List<PojoMethodParameterModel<?>> params = declaredParameters();
		if ( index >= params.size() ) {
			throw log.cannotFindConstructorParameter( this, index );
		}
		return params.get( index );
	}

	@Override
	public List<PojoMethodParameterModel<?>> declaredParameters() {
		if ( declaredParameters == null ) {
			declaredParameters = new ArrayList<>();
			Parameter[] parameters = constructor.getParameters();
			AnnotatedType[] annotatedTypes = constructor.getAnnotatedParameterTypes();

			Annotation[][] parameterAnnotationsArray = constructor.getParameterAnnotations();
			Annotation[][] annotationsForJDK8303112 =
					recomputeParameterAnnotationsForJDK8303112( parameters, parameterAnnotationsArray );

			for ( int i = 0; i < parameters.length; i++ ) {
				declaredParameters.add( new PojoHCAnnMethodParameterModel<>( this, i, parameters[i], annotatedTypes[i],
						annotationsForJDK8303112 != null ? annotationsForJDK8303112[i] : null ) );
			}
		}
		return declaredParameters;
	}

	/**
	 * This is a workaround for <a href="https://bugs.openjdk.org/browse/JDK-8303112">JDK-8303112</a>.
	 * @param parameters The result of calling {@link Constructor#getParameters()}
	 * @param parameterAnnotationsArray The result of calling  {@link Constructor#getParameterAnnotations()}
	 * @return A fixed version of {@code parameterAnnotationsArray},
	 * or {@code null} if {@code parameterAnnotationsArray} is fine an unaffected by JDK-8303112.
	 */
	private static Annotation[][] recomputeParameterAnnotationsForJDK8303112(Parameter[] parameters,
			Annotation[][] parameterAnnotationsArray) {
		int parameterCount = parameters.length;
		if ( parameterAnnotationsArray.length == parameterCount ) {
			// Not affected by JDK-8303112
			return null;
		}

		// We're in a situation where parameter.getAnnotation()/parameter.getAnnotations()
		// is buggy when there are implicit/synthetic parameters,
		// because constructor.getParameterAnnotations() (wrongly) ignores implicit/synthetic parameters
		// while parameter.getAnnotations() (rightly) assumes they are present in the array.

		Annotation[][] annotationsForJDK8303112;
		annotationsForJDK8303112 = new Annotation[parameterCount][];
		int nonImplicitNorSyntheticParamIndex = 0;
		for ( int i = 0; i < parameterCount; i++ ) {
			Parameter parameter = parameters[i];
			if ( parameter.isImplicit() || parameter.isSynthetic() ) {
				annotationsForJDK8303112[i] = new Annotation[0];
			}
			else if ( nonImplicitNorSyntheticParamIndex < parameterAnnotationsArray.length ) {
				annotationsForJDK8303112[i] =
						parameterAnnotationsArray[nonImplicitNorSyntheticParamIndex];
				++nonImplicitNorSyntheticParamIndex;
			}
			else {
				// Something is wrong; most likely the class wasn't compiled with -parameters
				// and so isImplicit/isSynthetic always return false.
				// As a last resort, assume the implicit/synthetic parameters are the first ones.
				nonImplicitNorSyntheticParamIndex = parameterCount - parameterAnnotationsArray.length;
				Arrays.fill( annotationsForJDK8303112, 0, nonImplicitNorSyntheticParamIndex,
						new Annotation[0] );
				System.arraycopy( parameterAnnotationsArray, 0, annotationsForJDK8303112,
						nonImplicitNorSyntheticParamIndex, parameterAnnotationsArray.length );
				return annotationsForJDK8303112;
			}
		}
		return annotationsForJDK8303112;
	}

	@Override
	public Class<?>[] parametersJavaTypes() {
		return constructor.getParameterTypes();
	}
}
