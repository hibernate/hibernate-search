/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan.spi;

/**
 * A DirectoryProvider using Infinispan to store the Index. This depends on the
 * CacheManagerServiceProvider to get a reference to the Infinispan {@link EmbeddedCacheManager}.
 *
 * @deprecated this implementation is now maintained by the Infinispan project: use {@link org.infinispan.hibernate.search.spi.InfinispanDirectoryProvider}
 * instead.
 *
 * @author Sanne Grinovero
 */
@Deprecated
public class InfinispanDirectoryProvider extends org.infinispan.hibernate.search.spi.InfinispanDirectoryProvider {

}
