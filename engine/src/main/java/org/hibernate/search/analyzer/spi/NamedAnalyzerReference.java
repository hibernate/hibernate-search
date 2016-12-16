/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.spi;

/**
 * A reference to an analyzer that also provides a name for this analyzer.
 *
 * @author Yoann Rodiere
 *
 * @hsearch.experimental This type is under active development as part of the Elasticsearch integration. You
 * should be prepared for incompatible changes in future releases.
 */
public interface NamedAnalyzerReference extends AnalyzerReference {

	String getAnalyzerName();

}
