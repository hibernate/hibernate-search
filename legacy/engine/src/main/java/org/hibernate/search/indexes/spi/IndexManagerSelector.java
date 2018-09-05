/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

import java.io.Serializable;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * @author Yoann Rodiere
 */
public interface IndexManagerSelector {

	/**
	 * Ask for all shards (eg to optimize)
	 * @return all the {@link IndexManager} for all shards
	 */
	Set<IndexManager> all();

	/**
	 * @param typeId the type of the entity
	 * @param id the id in object form
	 * @param idInString the id as transformed by the used TwoWayStringBridge
	 * @param document the document to index
	 * @return the IndexManager where the given entity will be indexed
	 */
	IndexManager forNew(IndexedTypeIdentifier typeId, Serializable id, String idInString, Document document);

	/**
	 * @param typeId the type of the existing entity
	 * @param id the id in object form
	 * @param idInString the id as transformed by the used TwoWayStringBridge
	 * @return If id and idInString are non-null, the {@link IndexManager}(s) where the given entity is stored.
	 * If they are null, all the {@link IndexManager}(s) where an entity of the given type is stored.
	 */
	Set<IndexManager> forExisting(IndexedTypeIdentifier typeId, Serializable id, String idInString);

	/**
	 * @param fullTextFilters the filters applied to the query; must be empty if no filter is applied
	 * @return the set of {@link IndexManager}(s) where the entities matching the filters are stored
	 */
	Set<IndexManager> forFilters(FullTextFilterImplementor[] fullTextFilters);

}
