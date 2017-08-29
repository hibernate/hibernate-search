/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.engine.logging.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.bridge.declaration.spi.BridgeMapping;
import org.hibernate.search.util.SearchException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH")
public interface Log extends BasicLogger {

	@Message(id = 1, value = "Unable to create annotation for definition of type %1$s")
	SearchException unableToCreateAnnotationForDefinition(Class<? extends Annotation> annotationType, @Cause Exception e);

	@Message(id = 2, value = "Unable to find a default identifier bridge implementation for type '%1$s'")
	SearchException unableToResolveDefaultIdentifierBridgeFromSourceType(Class<?> sourceType);

	@Message(id = 3, value = "Unable to find a default function bridge implementation for type '%1$s'")
	SearchException unableToResolveDefaultFunctionBridgeFromSourceType(Class<?> sourceType);

	@Message(id = 4, value = "A annotation of type '%1$s' was passed as a bridge definition,"
					+ " but this annotation type is missing the '%2$s' meta-annotation.")
	SearchException unableToResolveBridgeFromAnnotationType(Class<? extends Annotation> annotationType,
			Class<BridgeMapping> bridgeMappingAnnotationClass);

}
