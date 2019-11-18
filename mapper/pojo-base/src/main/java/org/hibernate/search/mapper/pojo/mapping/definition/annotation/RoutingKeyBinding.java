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

import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.RoutingKeyBindingProcessor;

/**
 * Maps an indexed type to its routing keys using a {@link RoutingKeyBinder},
 * which will define a {@link RoutingKeyBridge}.
 * <p>
 * See the reference documentation for more information about bridges in general,
 * and routing key bridges in particular.
 *
 * @hsearch.experimental This type is under active development.
 *    Usual compatibility policies do not apply: incompatible changes may be introduced in any future release.
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RoutingKeyBinding.List.class)
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = RoutingKeyBindingProcessor.class))
public @interface RoutingKeyBinding {

	/**
	 * @return A reference to the binder to use.
	 * @see RoutingKeyBinderRef
	 */
	RoutingKeyBinderRef binder();

	@Documented
	@Target({ ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		RoutingKeyBinding[] value();
	}

}
