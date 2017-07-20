/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch.model;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;

@Indexed
public class BookEntity3 extends AbstractBookEntity {

	public static final IndexedTypeIdentifier TYPE_ID = new PojoIndexedTypeIdentifier( BookEntity3.class );

}
