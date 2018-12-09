/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.converter.spi.ToIndexIdValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.mapper.model.spi.SearchModel;

public interface IndexModelBindingContext {

	Collection<IndexObjectFieldAccessor> getParentIndexObjectAccessors();

	IndexSchemaElement getSchemaElement();

	IndexSchemaElement getSchemaElement(IndexSchemaContributionListener listener);

	SearchModel getSearchModel();

	/**
	 * Inform the model collector that documents will always be provided along
	 * with an explicit routing key,
	 * to be used to route the document to a specific shard.
	 */
	void explicitRouting();

	void idDslConverter(ToIndexIdValueConverter<?> idConverter);

	Optional<IndexModelBindingContext> addIndexedEmbeddedIfIncluded(MappableTypeModel parentTypeModel,
			String relativePrefix, ObjectFieldStorage storage, Integer maxDepth, Set<String> includePaths);
}
