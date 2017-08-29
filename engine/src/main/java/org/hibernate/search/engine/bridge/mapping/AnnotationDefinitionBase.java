/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.bridge.mapping;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
/*
 * TODO add support for annotation parameters (?)
 * TODO add support for multi-valued parameters (?)
 * see https://github.com/hibernate/hibernate-validator/blob/master/engine/src/main/java/org/hibernate/validator/cfg/AnnotationDef.java
 * see https://github.com/hibernate/hibernate-validator/blob/master/engine/src/main/java/org/hibernate/validator/cfg/ConstraintDef.java
 * see NotNullDef in https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/?v=5.4#section-programmatic-api
 */
public abstract class AnnotationDefinitionBase<A extends Annotation> {

	private static final Log log = LoggerFactory.make( Log.class );

	protected final Map<String, Object> parameters = new HashMap<>();

	public A get() {
		Class<A> annotationClass = getAnnotationClass();
		AnnotationDescriptor annotationDescriptor = new AnnotationDescriptor( annotationClass );
		for ( Map.Entry<String, Object> parameter : parameters.entrySet() ) {
			annotationDescriptor.setValue( parameter.getKey(), parameter.getValue() );
		}

		try {
			return AnnotationFactory.create( annotationDescriptor );
		}
		catch (RuntimeException e) {
			throw log.unableToCreateAnnotationForDefinition( annotationClass, e );
		}
	}

	protected abstract Class<A> getAnnotationClass();

	protected final void addParameter(String name, Object value) {
		parameters.put( name, value );
	}

}
