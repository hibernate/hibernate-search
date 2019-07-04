/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBridgeRef;

@TypeBinding(
		bridge = @TypeBridgeRef(
				binderType = org.hibernate.search.integrationtest.showcase.library.bridge.AccountBorrowalSummaryBridge.Binder.class
		)
)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Documented
public @interface AccountBorrowalSummaryBridge {

}
