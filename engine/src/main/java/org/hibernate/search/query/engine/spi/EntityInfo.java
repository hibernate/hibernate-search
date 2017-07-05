/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.spi;

import java.io.Serializable;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * Wrapper class describing the loading of an element.
 *
 * @author Emmanuel Bernard
 */
public interface EntityInfo {

	Object ENTITY_PLACEHOLDER = new Object() {

		@Override
		public String toString() {
			return "HSearch: Entity placeholder";
		}

	};

	/**
	 * @return The entity type.
	 * @throws SearchException If the entity class could not be retrieved from the indexed document.
	 */
	IndexedTypeIdentifier getType();

	/**
	 * @return The entity identifier.
	 * @throws SearchException If the entity identifier could not be retrieved from the indexed document.
	 */
	Serializable getId();

	String getIdName();

	Object[] getProjection();

	Object getEntityInstance();

	void populateWithEntityInstance(Object entity);
}
