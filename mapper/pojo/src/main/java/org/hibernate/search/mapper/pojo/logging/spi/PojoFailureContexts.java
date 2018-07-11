/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.logging.spi;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.logging.impl.PojoFailureContextMessages;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.engine.logging.spi.AbstractSimpleFailureContextElement;
import org.hibernate.search.util.FailureContext;
import org.hibernate.search.engine.logging.spi.FailureContexts;

import org.jboss.logging.Messages;

public final class PojoFailureContexts {

	private static final PojoFailureContextMessages MESSAGES = Messages.getBundle( PojoFailureContextMessages.class );

	private PojoFailureContexts() {
	}

	public static FailureContext fromType(PojoRawTypeModel<?> typeModel) {
		return FailureContexts.fromType( typeModel );
	}

	public static FailureContext fromPath(PojoModelPath unboundPath) {
		return FailureContext.create( new AbstractSimpleFailureContextElement<PojoModelPath>( unboundPath ) {
			@Override
			public String render(PojoModelPath param) {
				String pathString = param == null ? "" : param.toPathString();
				return MESSAGES.path( pathString );
			}
		} );
	}

	public static FailureContext fromAnnotation(Annotation annotation) {
		return FailureContext.create( new AbstractSimpleFailureContextElement<Annotation>( annotation ) {
			@Override
			public String render(Annotation annotation) {
				String annotationString = annotation.toString();
				return MESSAGES.annotation( annotationString );
			}
		} );
	}
}
