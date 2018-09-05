/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.bridge.AppliedOnTypeAwareBridge;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.spi.IgnoreAnalyzerBridge;

/**
 * Specifies a custom field bridge implementation
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target({ ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface FieldBridge {

	/**
	 * The implementation class to instantiate.
	 * <p>
	 * The given class must implement either {@link org.hibernate.search.bridge.FieldBridge} or
	 * {@link StringBridge}.
	 * <p>
	 * Additionally, the given class may implement more advanced interfaces,
	 * such as for instance {@link TwoWayFieldBridge}, {@link TwoWayStringBridge},
	 * {@link MetadataProvidingFieldBridge}, {@link IgnoreAnalyzerBridge},
	 * {@link AppliedOnTypeAwareBridge} or {@link ParameterizedBridge}.
	 */
	Class<?> impl() default void.class;

	/**
	 * Parameters to be passed to the bridge instance,
	 * if it implements {@link ParameterizedBridge}.
	 */
	Parameter[] params() default { };
}
