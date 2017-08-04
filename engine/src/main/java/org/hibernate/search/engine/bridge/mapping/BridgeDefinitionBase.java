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
public abstract class BridgeDefinitionBase<A extends Annotation> implements BridgeDefinition<A> {

	private static final Log log = LoggerFactory.make( Log.class );

	protected final Map<String, Object> parameters = new HashMap<>();

	@Override
	public A get() {
		AnnotationDescriptor annotationDescriptor = new AnnotationDescriptor( getAnnotationClass() );
		for ( Map.Entry<String, Object> parameter : parameters.entrySet() ) {
			annotationDescriptor.setValue( parameter.getKey(), parameter.getValue() );
		}

		try {
			return AnnotationFactory.create( annotationDescriptor );
		}
		catch (RuntimeException e) {
			throw log.unableToCreateAnnotationForBridgeDefinition( e );
		}
	}

	protected abstract Class<A> getAnnotationClass();

	protected final void addParameter(String name, Object value) {
		parameters.put( name, value );
	}

}
