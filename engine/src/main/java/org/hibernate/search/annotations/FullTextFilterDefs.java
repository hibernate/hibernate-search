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
 * A list of FullTextFilterDef
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.PACKAGE, ElementType.TYPE } )
@Documented
public @interface FullTextFilterDefs {
	FullTextFilterDef[] value();
}
