/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.bridge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeMappingBuilderReference;
import org.hibernate.search.integrationtest.mapper.pojo.bridge.CustomPropertyBridge;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@PropertyBridgeMapping(builder = @PropertyBridgeMappingBuilderReference(type = CustomPropertyBridge.Builder.class))
public @interface CustomPropertyBridgeAnnotation {

	String objectName();

}
