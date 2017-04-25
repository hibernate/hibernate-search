/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.engine.service.spi.ServiceManager;

/**
 * Since Hibernate Search supports different types of indexing and query technologies,
 * such as embedding Apache Lucene or remote via Elasticsearch, the type of
 * such storage is represented by an instance implementing this interface.
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
public interface IndexManagerType {

	/**
	 * @param serviceManager the service manager
	 * @param cfg the Hibernate Search configuration, providing in particular access to configuration properties.
	 * @return a newly created strategy of analyzer execution employed by index managers of this family.
	 */
	AnalyzerStrategy createAnalyzerStrategy(ServiceManager serviceManager, SearchConfiguration cfg);

	/**
	 * @param serviceManager the service manager
	 * @param cfg the Hibernate Search configuration, providing in particular access to configuration properties.
	 * @return a newly created strategy for missing values employed by index managers of this family.
	 */
	MissingValueStrategy createMissingValueStrategy(ServiceManager serviceManager, SearchConfiguration cfg);
}
