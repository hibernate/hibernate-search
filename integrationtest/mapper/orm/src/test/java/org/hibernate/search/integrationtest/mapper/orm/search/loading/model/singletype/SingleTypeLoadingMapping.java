/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;

public abstract class SingleTypeLoadingMapping {

	public static List<SingleTypeLoadingMapping> all() {
		return Arrays.asList(
				new EntityIdDocumentIdMapping(),
				new NonEntityIdDocumentIdMapping()
		);
	}

	@Override
	public final String toString() {
		return describe();
	}

	protected abstract String describe();

	public abstract void configure(SimpleSessionFactoryBuilder builder, SingleTypeLoadingModel<?> model);

	public abstract boolean isCacheLookupSupported();

	public abstract String getDocumentIdForEntityId(int id);

	public abstract Integer generateUniquePropertyForEntityId(int id);

}
