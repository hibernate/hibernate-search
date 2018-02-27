/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoValueNodeMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @author Yoann Rodiere
 */
public class PojoContainerNodeProcessorBuilder<C, T> extends AbstractPojoNodeProcessorBuilder<C> {

	private final ContainerValueExtractor<C, T> extractor;

	private final PojoValueNodeProcessorCollectionBuilder<T> valueNodeProcessorCollectionBuilder;

	PojoContainerNodeProcessorBuilder(
			PojoPropertyNodeProcessorBuilder<?, ? extends C> parent,
			PojoTypeModel<?> parentTypeModel, String propertyName,
			PojoTypeModel<T> elementTypeModel, ContainerValueExtractor<C, T> extractor,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext) {
		super( parent, contributorProvider, indexModelBinder, bindingContext );
		this.extractor = extractor;

		valueNodeProcessorCollectionBuilder = new PojoValueNodeProcessorCollectionBuilder<>(
				this, parentTypeModel, propertyName, elementTypeModel,
				contributorProvider, indexModelBinder, bindingContext
		);
	}

	public PojoValueNodeMappingCollector value() {
		return valueNodeProcessorCollectionBuilder;
	}

	@Override
	protected void appendSelfPath(StringBuilder builder) {
		builder.append( "[" ).append( extractor ).append( "]" );
	}

	@Override
	Optional<PojoContainerNodeProcessor<C, T>> build() {
		Collection<PojoNodeProcessor<? super T>> immutableNestedProcessors =
				valueNodeProcessorCollectionBuilder.build();

		if ( immutableNestedProcessors.isEmpty() ) {
			/*
			 * If this processor doesn't have any bridge, nor any nested processor,
			 * it is useless and we don't need to build it
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoContainerNodeProcessor<>(
					extractor, immutableNestedProcessors
			) );
		}
	}
}
