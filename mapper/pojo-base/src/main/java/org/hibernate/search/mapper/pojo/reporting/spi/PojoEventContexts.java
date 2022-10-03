/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.reporting.spi;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoEventContextMessages;
import org.hibernate.search.util.common.reporting.impl.AbstractSimpleEventContextElement;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;

public final class PojoEventContexts {

	private static final PojoEventContextMessages MESSAGES = PojoEventContextMessages.INSTANCE;

	private PojoEventContexts() {
	}

	public static EventContext fromType(PojoRawTypeModel<?> typeModel) {
		return EventContexts.fromType( typeModel );
	}

	public static EventContext fromType(PojoRawTypeIdentifier<?> typeIdentifier) {
		return EventContexts.fromType( typeIdentifier );
	}

	public static EventContext fromConstructor(PojoConstructorModel<?> constructor) {
		return EventContext.create( new AbstractSimpleEventContextElement<PojoConstructorModel<?>>( constructor ) {
			@Override
			public String render(PojoConstructorModel<?> constructor) {
				return MESSAGES.constructor( constructor.parametersJavaTypes() );
			}
		} );
	}

	public static EventContext fromMethodParameter(PojoMethodParameterModel<?> parameter) {
		return EventContext.create( new AbstractSimpleEventContextElement<PojoMethodParameterModel<?>>( parameter ) {
			@Override
			public String render(PojoMethodParameterModel<?> parameter) {
				return MESSAGES.methodParameter( parameter.index(), parameter.name().orElse( MESSAGES.unknownName() ) );
			}
		} );
	}

	public static EventContext fromPath(PojoModelPath unboundPath) {
		return EventContext.create( new AbstractSimpleEventContextElement<PojoModelPath>( unboundPath ) {
			@Override
			public String render(PojoModelPath param) {
				String pathString = param == null ? "" : param.toPathString();
				return MESSAGES.path( pathString );
			}
		} );
	}

	public static EventContext fromAnnotation(Annotation annotation) {
		return EventContext.create( new AbstractSimpleEventContextElement<Annotation>( annotation ) {
			@Override
			public String render(Annotation annotation) {
				String annotationString = annotation.toString();
				return MESSAGES.annotation( annotationString );
			}
		} );
	}

	public static EventContext fromAnnotationType(Class<? extends Annotation> annotationType) {
		return EventContext.create( new AbstractSimpleEventContextElement<Class<? extends Annotation>>( annotationType ) {
			@Override
			public String render(Class<? extends Annotation> annotationType) {
				return MESSAGES.annotationType( annotationType );
			}
		} );
	}
}
