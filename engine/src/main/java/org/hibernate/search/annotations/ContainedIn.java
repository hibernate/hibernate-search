/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * Describe the owning entity as being part of the target entity's
 * index (to be more accurate, being part of the indexed object graph).
 * <p>
 * Only necessary when an &#64;Indexed class is used as a &#64;IndexedEmbedded
 * target class. &#64;ContainedIn must mark the property pointing back
 * to the &#64;IndexedEmbedded owning Entity.
 * <p>
 * Not necessary if the class is an &#64;Embeddable class.
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
