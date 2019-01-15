/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.declaration;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;

/**
 * Allows to map a property bridge to an annotation type,
 * so that whenever the annotation is found on a field or method in the domain model,
 * the property bridge mapped to the annotation will be applied.
 */
@Documented
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyBridgeMapping {

	/**
	 * Map a property bridge to an annotation type.
	 * <p>
	 * Each time the mapped annotation is encountered, an instance of the property bridge will be created
	 * and applied to the location where the annotation was found.
	 * <p>
	 * Property bridges mapped this way cannot be parameterized:
	 * any attribute of the mapped annotation will be ignored.
	 * See {@link #builder()} to take advantage of the annotation attributes.
	 * <p>
	 * This attribute cannot be used in the same {@link PropertyBridgeMapping} annotation
	 * as {@link #builder()}: either a bridge or a bridge builder can be provided, but never both.
	 *
	 * @return A reference to the property bridge to use.
	 */
	PropertyBridgeRef bridge() default @PropertyBridgeRef;

	/**
	 * Map a property bridge builder to an annotation type.
	 * <p>
	 * Each time the mapped annotation is encountered, an instance of the property bridge builder will be created.
	 * The builder will be passed the annotation through its
	 * {@link AnnotationBridgeBuilder#initialize(Annotation)} method,
	 * and then the bridge will be retrieved by calling {@link BridgeBuilder#build(org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext)}.
	 * <p>
	 * Property bridges mapped this way can be parameterized:
	 * the bridge will be able to take any attribute of the mapped annotation into account
	 * in its {@link AnnotationBridgeBuilder#initialize(Annotation)} method.
	 * <p>
	 * This attribute cannot be used in the same {@link PropertyBridgeMapping} annotation
	 * as {@link #bridge()}: either a bridge or a bridge builder can be provided, but never both.
	 *
	 * @return A reference to the builder to use to build the property bridge.
	 */
	PropertyBridgeAnnotationBuilderReference builder() default @PropertyBridgeAnnotationBuilderReference;

}
