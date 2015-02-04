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

/**
 * Marks a method as a factory method for a given type. A factory method is called whenever a new instance of a given
 * type is requested. The factory method is used with a higher priority than a plain no-arg constructor when present.
 * <p>
 * Factory methods currently are used to instantiate the following classes:
 * <ul>
 * <li>{@link org.apache.lucene.search.Filter}; The factory class is to be given via {@link FullTextFilterDef#impl()}</li>
 * <li>{@link org.hibernate.search.cfg.SearchMapping}; The factory class is to be registered with the bootstrapping
 * configuration using the {@link org.hibernate.search.cfg.Environment#MODEL_MAPPING} key</li>
 * </ul>
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@Documented
public @interface Factory {
}
