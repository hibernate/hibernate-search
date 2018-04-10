/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.spi;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;

/**
 * @author Yoann Rodiere
 */
public interface Backend<D extends DocumentElement> extends AutoCloseable {

	/**
	 * Normalize the name of the index, so that we cannot end up with two index names in Hibernate Search
	 * that would target the same physical index.
	 *
	 * @param rawIndexName The index name to be normalized.
	 * @return The normalized index name.
	 */
	String normalizeIndexName(String rawIndexName);

	/**
	 * @param normalizedIndexName The (already {@link #normalizeIndexName(String) normalized}) name of the index
	 * @param context The build context
	 * @param propertySource A configuration property source, appropriately masked so that the backend
	 * doesn't need to care about Hibernate Search prefixes (hibernate.search.*, etc.). All the properties
	 * can be accessed at the root.
	 * <strong>CAUTION:</strong> the property key {@code backend} is reserved for use by the engine.
	 * @return A builder for index managers targeting this backend.
	 */
	IndexManagerBuilder<D> createIndexManagerBuilder(String normalizedIndexName, BuildContext context,
			ConfigurationPropertySource propertySource);

	@Override
	void close();

}
