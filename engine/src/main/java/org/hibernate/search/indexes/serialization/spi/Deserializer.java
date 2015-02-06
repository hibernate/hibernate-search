/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.spi;

/**
 * Contract between Hibernate Search and the {@code LuceneWork} deserializer.
 *
 * All {@code LuceneWork} construction is delegated to methods of {@code LuceneWorksBuilder}.
 * After {@code deserialize} is called, Hibernate Search will
 * extract the built List of {@code LuceneWork} instances.
 *
 * @see org.hibernate.search.indexes.serialization.impl.LuceneWorkHydrator
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface Deserializer {
	void deserialize(byte[] data, LuceneWorksBuilder hydrator);
}
