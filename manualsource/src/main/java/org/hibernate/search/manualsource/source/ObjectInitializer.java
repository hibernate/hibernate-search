/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.source;

import java.util.List;
import java.util.Map;

import org.hibernate.search.query.engine.spi.TimeoutManager;

/**
 * Interface implemented by the source of data to load specific entities.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface ObjectInitializer {
	/**
	 * Initializes the set of requested objects and put them in the map.
	 */
	void initializeObjects(List<EntityKeyForLoad> keys, Map<EntityKeyForLoad, Object> idsToObjects, Context context);

	interface Context {
		EntitySourceContext getEntitySourceContext();
		TimeoutManager getTimeoutManager();
	}
}
