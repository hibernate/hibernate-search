/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.declaration;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;

/**
 * Reference a property bridge for a {@link TypeBridgeMapping}.
 * <p>
 * Either a bridge or an annotation bridge builder can be provided, but never both.
 * Reference can be obtained using either a name or a type.
 * <p>
 * If a <b>direct bridge</b> is provided, using the methods {@link #name()} or {@link #type()},
 * each time the mapped annotation is encountered, an instance of the property bridge will be created.
 * <p>
 * Property bridges mapped this way cannot be parameterized:
 * any attribute of the mapped annotation will be ignored.
 * <p>
 * If an <b>annotation bridge builder</b> is provided, using the methods {@link #builderName()} or {@link #builderType()},
 * each time the mapped annotation is encountered, an instance of the property bridge builder will be created.
 * The builder will be passed the annotation through its {@link AnnotationBridgeBuilder#initialize(Annotation)} method,
 * and then the bridge will be retrieved by calling {@link BridgeBuilder#build(org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext)}.
 * <p>
 * Property bridges mapped this way can be parameterized:
 * the bridge will be able to take any attribute of the mapped annotation into account
 * in its {@link AnnotationBridgeBuilder#initialize(Annotation)} method.
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyBridgeRef {

	/**
	 * Reference a property bridge by its bean name.
	 * @return The bean name of the  property bridge.
	 */
	String name() default "";

	/**
	 * Reference a property bridge by its type.
	 * @return The type of the  property bridge.
	 */
	Class<? extends PropertyBridge> type() default UndefinedBridgeImplementationType.class;

	/**
	 * Reference a property bridge by the bean name of its builder.
	 * @return The bean name of the  property bridge builder.
	 */
	String builderName() default "";

	/**
	 * Reference a property bridge by the type of its builder.
	 * @return The type of the  property bridge builder.
	 */
	Class<? extends AnnotationBridgeBuilder<? extends PropertyBridge,?>> builderType() default UndefinedBuilderImplementationType.class;

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedBridgeImplementationType implements PropertyBridge {
		private UndefinedBridgeImplementationType() {
		}
	}

	/**
	 * Class used as a marker for the default value of the {@link #builderType()} attribute.
	 */
	abstract class UndefinedBuilderImplementationType implements AnnotationBridgeBuilder<PropertyBridge, Annotation> {
		private UndefinedBuilderImplementationType() {
		}
	}
}
