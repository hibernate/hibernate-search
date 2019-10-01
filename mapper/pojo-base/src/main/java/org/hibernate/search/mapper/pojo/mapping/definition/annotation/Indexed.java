/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps an entity type to an index.
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Indexed {

	/**
	 * @return The name of the backend.
	 * Defaults to the {@link org.hibernate.search.engine.cfg.EngineSettings#DEFAULT_BACKEND default backend}.
	 */
	String backend() default "";

	/**
	 * @return The name of the index.
	 * Defaults to the entity name.
	 */
	String index() default "";

}
