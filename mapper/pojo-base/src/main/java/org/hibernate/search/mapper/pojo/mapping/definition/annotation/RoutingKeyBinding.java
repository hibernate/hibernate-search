/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.RoutingKeyBindingProcessor;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Maps an indexed type to its routing keys using a {@link org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder},
 * which will define a {@link org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge}.
 * <p>
 * See the reference documentation for more information about bridges in general,
 * and routing key bridges in particular.
 * @deprecated Apply a {@link org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder}
 * with {@link Indexed#routingBinder()} instead.
 */
@Deprecated
@Documented
@Incubating
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RoutingKeyBinding.List.class)
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = RoutingKeyBindingProcessor.class))
public @interface RoutingKeyBinding {

	/**
	 * @return A reference to the binder to use.
	 * @see org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBinderRef
	 */
	org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBinderRef binder();

	@Documented
	@Target({ ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		RoutingKeyBinding[] value();
	}

}
