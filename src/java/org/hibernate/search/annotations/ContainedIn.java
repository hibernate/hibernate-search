//$Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * Describe the owning entity as being part of the target entity's
 * index (to be more accurate, being part of the indexed object graph)
 *
 * Only necessary when an @Indexed class is used as a @IndexedEmbedded
 * target class. @ContainedIn must mark the property pointing back
 * to the @IndexedEmbedded owning Entity
 *
 * Not necessary if the class is an @Embeddable class.
 *
 * <code>
 * @Indexed
 * public class OrderLine {
 *     @IndexedEmbedded
 *     private Order order;
 * }
 *
 * @Indexed
 * public class Order {
 *     @ContainedBy
 *     Set<OrderLine> lines;
 * }
 * </code>
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.FIELD, ElementType.METHOD} )
@Documented
public @interface ContainedIn {
}
