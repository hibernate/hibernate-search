/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.indexes.IndexFamily;

/**
 * The SPI contract for {@link IndexFamily} implementors.
 *
 * @hsearch.experimental This type is under active development as part of the Elasticsearch integration.
 *    You should be prepared for incompatible changes in future releases.
 */
public interface IndexFamilyImplementor extends IndexFamily, AutoCloseable {

	/**
	 * @return a newly created strategy of analyzer execution employed by index managers of this family.
	 */
	AnalyzerStrategy createAnalyzerStrategy();

	/**
	 * @return the missing value strategy employed by index managers of this family.
	 */
	MissingValueStrategy getMissingValueStrategy();

	@Override
	void close();

}
