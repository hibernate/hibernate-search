/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.interceptor;

import javax.persistence.Entity;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.indexes.interceptor.DontInterceptEntityInterceptor;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
@Indexed(interceptor = DontInterceptEntityInterceptor.class)
public class TotalArticle extends Article {
}
