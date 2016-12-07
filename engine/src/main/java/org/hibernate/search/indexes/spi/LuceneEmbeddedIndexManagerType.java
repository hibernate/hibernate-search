/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

import org.hibernate.search.engine.nulls.impl.LuceneMissingValueStrategy;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;

public final class LuceneEmbeddedIndexManagerType implements IndexManagerType {

	public static final LuceneEmbeddedIndexManagerType INSTANCE = new LuceneEmbeddedIndexManagerType();

	private LuceneEmbeddedIndexManagerType() {
		//use the INSTANCE singleton
	}

	@Override
	public MissingValueStrategy getMissingValueStrategy() {
		return LuceneMissingValueStrategy.INSTANCE;
	}

	@Override
	public MissingValueStrategy getContainerMissingValueStrategy() {
		return LuceneMissingValueStrategy.INSTANCE;
	}

	@Override
	public AnalyzerExecutionStrategy getAnalyzerExecutionStrategy() {
		return AnalyzerExecutionStrategy.EMBEDDED;
	}
}
