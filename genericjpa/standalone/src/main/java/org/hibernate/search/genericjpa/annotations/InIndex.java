/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation is needed to specify that a class is located in an index. <br>
 * <br>
 * This is only needed (but always allowed), if objects of subclasses are supplied to indexing related operations. Then
 * the "least top level" class that is annotated with @InIndex is used. operations
 *
 * @author Martin
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface InIndex {

}
