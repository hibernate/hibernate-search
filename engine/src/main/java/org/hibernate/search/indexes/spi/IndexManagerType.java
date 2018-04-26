/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.IndexFamilyType;

/**
 * The SPI contract for {@link IndexFamilyType}, i.e. for representations of indexing and query technologies,
 * such as embedding Apache Lucene or remote via Elasticsearch.
 * <p>
 * From the point of view of Hibernate Search, an {@code IndexManagerType} is the same as an {@link IndexFamilyType},
 * but {@link IndexFamilyType} is part of Hibernate Search APIs
 * and we simply don't want to expose the methods of {@code IndexManagerType} to users.
 * <p>
 * Instances of implementations of this interface could be used as keys in a Map,
 * so make sure to implement appropriate equals and hashCode functions.
 * <p>
 * The purpose is that some components will have to adapt their output depending
 * on the target technology being used, so they might need to know the type.
 * We refrain from using Enums as that would not be extensible, and avoid using
 * the class type of an IndexManager to allow creating multiple custom
 * implementations for the same type of technology.
 *
 * @author Sanne Grinovero
 * @author Gunnar Morling
 * @hsearch.experimental This type is under active development as part of the Elasticsearch integration.
 *    You should be prepared for incompatible changes in future releases.
 * @since 5.6
 */
public interface IndexManagerType extends IndexFamilyType {

	/**
	 * @param serviceManager the service manager
	 * @param cfg the Hibernate Search configuration, providing in particular access to configuration properties.
	 * @return a newly created index family.
	 */
	IndexFamilyImplementor createIndexFamily(ServiceManager serviceManager, SearchConfiguration cfg);
}
