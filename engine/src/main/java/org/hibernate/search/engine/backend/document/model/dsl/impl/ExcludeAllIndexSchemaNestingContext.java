/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl.impl;

import java.util.function.BiFunction;
import java.util.function.Function;



class ExcludeAllIndexSchemaNestingContext implements IndexSchemaNestingContext {

	static final ExcludeAllIndexSchemaNestingContext INSTANCE = new ExcludeAllIndexSchemaNestingContext();

	private ExcludeAllIndexSchemaNestingContext() {
	}

	@Override
	public <T> T nest(String relativeFieldName, Function<String, T> nestedElementFactoryIfIncluded,
			Function<String, T> nestedElementFactoryIfExcluded) {
		return nestedElementFactoryIfExcluded.apply( relativeFieldName );
	}

	@Override
	public <T> T nest(String relativeFieldName,
			BiFunction<String, IndexSchemaNestingContext, T> nestedElementFactoryIfIncluded,
			BiFunction<String, IndexSchemaNestingContext, T> nestedElementFactoryIfExcluded) {
		return nestedElementFactoryIfExcluded.apply( relativeFieldName, INSTANCE );
	}

}
