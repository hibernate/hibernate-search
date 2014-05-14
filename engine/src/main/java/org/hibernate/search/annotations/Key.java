/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * Marks a method as a key constructor for a given type.
 * A key is an object that uniquely identify a given object type and a given set of parameters
 *
 * The key object must implement equals / hashcode so that 2 keys are equals iif
 * the given target object types are the same, the set of parameters are the same.
 *
 * &#64;Factory currently works for &#64;FullTextFilterDef.impl classes
 *
 * @see org.hibernate.search.annotations.Factory
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@Documented
public @interface Key {
}
