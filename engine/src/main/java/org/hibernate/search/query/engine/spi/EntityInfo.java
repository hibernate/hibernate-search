/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.spi;

import java.io.Serializable;

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

	Class<?> getClazz();

	Serializable getId();

	String getIdName();

	Object[] getProjection();

	Object getEntityInstance();

	void populateWithEntityInstance(Object entity);
}
