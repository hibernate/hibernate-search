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

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

/**
 * Define an Analyzer for a given entity, method, field or Field
 * The order of precedence is as such:
 *  - @Field
 *  - field / method
 *  - default
 *
 * @author Emmanuel Bernard
 *
 * @deprecated No longer necessary in Hibernate Search 6.
 * Replace {@link Field} with {@link FullTextField} and pass the analyzer name directly to {@link FullTextField#analyzer()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Documented
@Deprecated
public @interface Analyzer {
	String definition() default "";
}
