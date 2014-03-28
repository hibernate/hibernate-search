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
 * Describe the owning entity as being part of the target entity's
 * indexed object graph.
 * <p>
 * Often used when an &#64;Indexed class is used as a &#64;IndexedEmbedded
 * target class. &#64;ContainedIn must mark the property pointing back
 * to the &#64;IndexedEmbedded owning Entity (not necessary if the class
 * is an &#64;Embeddable class).
 * <p>
 * Also used to trigger a reindex of related entities even if no
 * &#64;IndexedEmbedded is involved, allowing to define a dependency
 * graph.
 * <p>
 * <code>
 * &#64;Indexed<br>
 * public class OrderLine {<br>
 *     &#64;IndexedEmbedded<br>
 *     private Order order;<br>
 * }<br>
 *<br>
 * &#64;Indexed<br>
 * public class Order {<br>
 *     &#64;ContainedIn<br>
 *     Set<OrderLine> lines;<br>
 * }<br>
 * </code><br>
 *
 * @see org.hibernate.search.annotations.Indexed
 * @see org.hibernate.search.annotations.IndexedEmbedded
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target({ ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface ContainedIn {
}
