/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;

/**
 * A reference to an {@code ElasticsearchAnalyzer}.
 *
 * @author Yoann Rodiere
 */
public abstract class ElasticsearchAnalyzerReference extends RemoteAnalyzerReference {

	@Override
	public abstract ElasticsearchAnalyzer getAnalyzer();

}
