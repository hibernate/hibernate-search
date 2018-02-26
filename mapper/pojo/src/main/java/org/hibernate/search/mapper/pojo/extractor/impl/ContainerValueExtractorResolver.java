/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public class ContainerValueExtractorResolver {

	// TODO add an extension point to override the builtin extractors, or at least to add defaults for other types

	@SuppressWarnings("unchecked") // Checks are implemented using reflection
	public <C> Optional<BoundContainerValueExtractor<? super C, ?>> resolveContainerValueExtractorForType(
			PojoGenericTypeModel<C> sourceType) {
		Optional<? extends PojoGenericTypeModel<?>> elementTypeModelOptional =
				sourceType.getTypeArgument( Map.class, 1 );
		if ( elementTypeModelOptional.isPresent() ) {
			return Optional.of( new BoundContainerValueExtractor(
					MapValueValueExtractor.get(), elementTypeModelOptional.get()
			) );
		}
		elementTypeModelOptional = sourceType.getTypeArgument( Collection.class, 0 );
		if ( elementTypeModelOptional.isPresent() ) {
			return Optional.of( new BoundContainerValueExtractor(
					CollectionValueExtractor.get(), elementTypeModelOptional.get()
			) );
		}
		elementTypeModelOptional = sourceType.getTypeArgument( Iterable.class, 0 );
		if ( elementTypeModelOptional.isPresent() ) {
			return Optional.of( new BoundContainerValueExtractor(
					IterableValueExtractor.get(), elementTypeModelOptional.get()
			) );
		}

		return Optional.empty();
	}

}
