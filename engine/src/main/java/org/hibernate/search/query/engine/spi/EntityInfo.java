/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.spi;

import java.io.Serializable;
import java.util.List;

/**
 * Wrapper class describing the loading of an element.
 *
 * @author Emmanuel Bernard
 */
public interface EntityInfo {
	Class<?> getClazz();

	Serializable getId();

	String getIdName();

	Object[] getProjection();

	List<Integer> getIndexesOfThis();

	boolean isProjectThis();

	void populateWithEntityInstance(Object entity);
}
