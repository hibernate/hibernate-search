/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public final class PojoSingleLoaderLoadingPlan<T> implements PojoLoadingPlan<T> {

	private final PojoLoadingContext context;

	private final Set<PojoRawTypeIdentifier<? extends T>> expectedTypes = new LinkedHashSet<>();
	private final List<Object> identifiers = new ArrayList<>();

	private PojoLoader<? super T> loader;
	private List<?> loaded;

	public PojoSingleLoaderLoadingPlan(PojoLoadingContext context) {
		this.context = context;
	}

	@Override
	public int planLoading(PojoRawTypeIdentifier<? extends T> expectedType, Object identifier) {
		expectedTypes.add( expectedType );
		identifiers.add( identifier );
		return identifiers.size() - 1;
	}

	@Override
	public void loadBlocking(Deadline deadline) {
		context.checkOpen();
		if ( identifiers.isEmpty() ) {
			// Avoid creating and calling the loader:
			// it may be expensive even if there are no entities to load,
			// and we don't expect any call to getLoaded*() in this case.
			return;
		}
		try {
			loader = context.createLoader( expectedTypes );
			loaded = loader.loadBlocking( identifiers, deadline );
		}
		finally {
			expectedTypes.clear();
			identifiers.clear();
		}
	}

	@Override
	public <T2 extends T> T2 retrieve(PojoRawTypeIdentifier<T2> expectedType, int ordinal) {
		Object retrieved = loaded.get( ordinal );
		if ( retrieved == null ) {
			return null; // Couldn't be loaded.
		}
		return loader.castToExactTypeOrNull( expectedType, retrieved );
	}

	@Override
	public void clear() {
		expectedTypes.clear();
		identifiers.clear();
		loaded = null;
	}

}
