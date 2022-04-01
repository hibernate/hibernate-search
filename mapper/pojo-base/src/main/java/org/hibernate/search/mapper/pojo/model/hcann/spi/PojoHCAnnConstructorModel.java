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
		return getClass().getSimpleName() + "[" + declaringTypeModel.name() + "(" + CommaSeparatedClassesFormatter.format( parametersJavaTypes() ) + ")" + "]";
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
	public List<PojoMethodParameterModel<?>> declaredParameters() {
		if ( declaredParameters == null ) {
			declaredParameters = new ArrayList<>();
			Parameter[] parameters = constructor.getParameters();
			AnnotatedType[] annotatedTypes = constructor.getAnnotatedParameterTypes();
			for ( int i = 0; i < parameters.length; i++ ) {
				declaredParameters.add( new PojoHCAnnMethodParameterModel<>( this, i, parameters[i], annotatedTypes[i] ) );
			}
		}
		return declaredParameters;
	}

	@Override
	public Class<?>[] parametersJavaTypes() {
		return constructor.getParameterTypes();
	}
}
