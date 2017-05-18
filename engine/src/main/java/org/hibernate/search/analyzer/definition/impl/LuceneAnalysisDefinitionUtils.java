/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition.impl;

import java.util.List;
import java.util.function.IntFunction;

/**
 * @author Yoann Rodiere
 */
public final class LuceneAnalysisDefinitionUtils {

	private LuceneAnalysisDefinitionUtils() {
		// Cannot be instantiated.
	}

	public static <T> T[] buildAll(List<? extends LuceneAnalysisDefinitionBuilder<T>> builders, IntFunction<T[]> arraySupplier) {
		int index = 0;
		T[] result = arraySupplier.apply( builders.size() );
		for ( LuceneAnalysisDefinitionBuilder<T> builder : builders ) {
			result[index] = builder.build();
			++index;
		}
		return result;
	}

}
