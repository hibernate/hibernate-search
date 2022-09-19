/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.inject.scope.spi;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Scope;

/**
 * Scope for job execution, to be mapped to the scopes specific to each JSR-352 implementation.
 */
@Target({ TYPE })
@Retention(RUNTIME)
@Documented
@Scope
@Inherited
public @interface HibernateSearchJobScoped {

}
