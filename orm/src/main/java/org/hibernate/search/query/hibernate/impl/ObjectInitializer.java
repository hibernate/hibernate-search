/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.search.query.engine.spi.EntityInfo;

/**
 * Initializes the objects specified by an array of {@code EntityInfo} instances.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface ObjectInitializer {

	Object ENTITY_NOT_YET_INITIALIZED = new Object();

	/**
	 * Given a array of {@code}
	 *
	 * @param entityInfos the {@code EntityInfo} instances to initialize
	 * @param idToObjectMap map keeping to store the loaded entities in
	 * @param objectInitializationContext gives access to the resources needed in the context of entity initialization
	 */
	void initializeObjects(List<EntityInfo> entityInfos,
			LinkedHashMap<EntityInfoLoadKey, Object> idToObjectMap,
			ObjectInitializationContext objectInitializationContext);
}
