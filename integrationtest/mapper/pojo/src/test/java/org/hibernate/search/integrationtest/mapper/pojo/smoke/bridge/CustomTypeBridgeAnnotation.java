/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.smoke.bridge;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBridgeRef;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@TypeBinding(bridge = @TypeBridgeRef(binderType = CustomTypeBridge.Binder.class))
public @interface CustomTypeBridgeAnnotation {

	String objectName();

}
