/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan.spi;

import org.hibernate.search.engine.service.spi.Service;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * @author Hardy Ferentschik
 */
public interface CacheManagerService extends Service {

	EmbeddedCacheManager getEmbeddedCacheManager();

}
