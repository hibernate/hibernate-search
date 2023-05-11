/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoHCAnnMethodParameterModel<T> implements PojoMethodParameterModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoHCAnnConstructorModel<?> constructorModel;
	private final int index;
	private final Parameter parameter;
	private final AnnotatedType annotatedType;

	private PojoTypeModel<T> typeModelCache;

	public PojoHCAnnMethodParameterModel(PojoHCAnnConstructorModel<?> constructorModel, int index,
			Parameter parameter, AnnotatedType annotatedType) {
		this.constructorModel = constructorModel;
		this.index = index;
		this.parameter = parameter;
		this.annotatedType = annotatedType;
	}

	@Override
	public String toString() {
		return "parameter #" + index + "(" + name().orElse( "<unknown name>" ) + ")";
	}

	@Override
	public int index() {
		return index;
	}

	@Override
	public Optional<String> name() {
		return parameter.isNamePresent() ? Optional.of( parameter.getName() ) : Optional.empty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public PojoTypeModel<T> typeModel() {
		if ( typeModelCache == null ) {
			try {
				typeModelCache = (PojoTypeModel<T>) constructorModel.declaringTypeModel.rawTypeDeclaringContext
						.memberTypeReference( annotatedType.getType() );
			}
			catch (RuntimeException e) {
				throw log.errorRetrievingConstructorParameterTypeModel( index, constructorModel, e );
			}
		}
		return typeModelCache;
	}

	@Override
	public boolean isEnclosingInstance() {
		// HSEARCH-4853: we can't simply use `Parameter#isImplicit()` because, starting with JDK 21-ea+21,
		// this returns `true` for parameters of canonical constructors of record types.
		return index == 0
				&& constructorModel.declaringTypeModel.javaClass().getEnclosingClass() != null
				&& !Modifier.isStatic( constructorModel.declaringTypeModel.javaClass().getModifiers() );
	}
}
