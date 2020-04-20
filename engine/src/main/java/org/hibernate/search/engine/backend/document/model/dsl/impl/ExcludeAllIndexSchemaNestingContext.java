/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl.impl;


class ExcludeAllIndexSchemaNestingContext implements IndexSchemaNestingContext {

	static final ExcludeAllIndexSchemaNestingContext INSTANCE = new ExcludeAllIndexSchemaNestingContext();

	private ExcludeAllIndexSchemaNestingContext() {
	}

	@Override
	public <T> T nest(String relativeName, LeafFactory<T> factoryIfIncluded, LeafFactory<T> factoryIfExcluded) {
		return factoryIfExcluded.create( relativeName );
	}

	@Override
	public <T> T nest(String relativeName, CompositeFactory<T> factoryIfIncluded,
			CompositeFactory<T> factoryIfExcluded) {
		return factoryIfExcluded.create( relativeName, INSTANCE );
	}

}
