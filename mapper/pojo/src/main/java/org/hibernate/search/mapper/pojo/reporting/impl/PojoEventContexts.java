/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.reporting.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.reporting.impl.AbstractSimpleEventContextElement;
import org.hibernate.search.util.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;

import org.jboss.logging.Messages;

public final class PojoEventContexts {

	private static final PojoEventContextMessages MESSAGES = Messages.getBundle( PojoEventContextMessages.class );

	private PojoEventContexts() {
	}

	public static EventContext fromType(PojoRawTypeModel<?> typeModel) {
		return EventContexts.fromType( typeModel );
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
}
