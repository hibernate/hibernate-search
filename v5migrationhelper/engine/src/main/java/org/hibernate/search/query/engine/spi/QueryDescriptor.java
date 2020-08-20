/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.spi;

import org.hibernate.search.spi.CustomTypeMetadata;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.SearchIntegrator;

/**
 * An index query which is able to create a corresponding {@link HSQuery} object.
 *
 * <p><strong>Note:</strong>In the hibernate-search-orm module, you may pass query descriptors to
 * {@code org.hibernate.search.FullTextSession.createFullTextQuery(QueryDescriptor, Class<?>...)}
 * to create a FullTextQuery.
 *
 * @author Gunnar Morling
 */
public interface QueryDescriptor {

	/**
	 * @param integrator the {@link SearchIntegrator} used to execute the query
	 * @param types the list of classes (indexes) targeted by the query
	 * @return the resulting query
	 */
	HSQuery createHSQuery(SearchIntegrator integrator, IndexedTypeSet types);

	/**
	 * @param integrator the {@link SearchIntegrator} used to execute the query
	 * @param types the targeted types, mapped to (potentially null) custom metadata which should override the supporting entity type's metadata
	 * @return the resulting query
	 */
	HSQuery createHSQuery(SearchIntegrator integrator, IndexedTypeMap<CustomTypeMetadata> types);

}
