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
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;

public class ContainerValueExtractorResolver {

	// TODO add an extension point to override the builtin extractors, or at least to add defaults for other types

	@SuppressWarnings("unchecked") // Checks are implemented using reflection
	public <C> Optional<BoundContainerValueExtractor<? super C, ?>> resolveContainerValueExtractorForType(
			PojoIntrospector introspector, PojoGenericTypeModel<C> sourceType) {
		return resolveContainerValueExtractorRecursively(
				new ExtractorResolutionState<>( introspector, sourceType )
		);
	}

	private <C> Optional<BoundContainerValueExtractor<? super C, ?>> resolveContainerValueExtractorRecursively(
			ExtractorResolutionState<C> state) {
		PojoGenericTypeModel<?> currentType = state.extractedType;
		Optional<? extends PojoGenericTypeModel<?>> elementTypeModelOptional =
				currentType.getTypeArgument( Map.class, 1 );
		if ( elementTypeModelOptional.isPresent() ) {
			state.append( MapValueValueExtractor.get(), elementTypeModelOptional.get() );
			return resolveContainerValueExtractorRecursively( state );
		}
		elementTypeModelOptional = currentType.getTypeArgument( Collection.class, 0 );
		if ( elementTypeModelOptional.isPresent() ) {
			state.append( CollectionValueExtractor.get(), elementTypeModelOptional.get() );
			return resolveContainerValueExtractorRecursively( state );
		}
		elementTypeModelOptional = currentType.getTypeArgument( Iterable.class, 0 );
		if ( elementTypeModelOptional.isPresent() ) {
			state.append( IterableValueExtractor.get(), elementTypeModelOptional.get() );
			return resolveContainerValueExtractorRecursively( state );
		}
		elementTypeModelOptional = currentType.getTypeArgument( Optional.class, 0 );
		if ( elementTypeModelOptional.isPresent() ) {
			state.append( OptionalValueExtractor.get(), elementTypeModelOptional.get() );
			return resolveContainerValueExtractorRecursively( state );
		}
		if ( currentType.getSuperType( OptionalInt.class ).isPresent() ) {
			state.append(
					OptionalIntValueExtractor.get(),
					state.introspector.getGenericTypeModel( Integer.class )
			);
			return resolveContainerValueExtractorRecursively( state );
		}
		if ( currentType.getSuperType( OptionalLong.class ).isPresent() ) {
			state.append(
					OptionalLongValueExtractor.get(),
					state.introspector.getGenericTypeModel( Long.class )
			);
			return resolveContainerValueExtractorRecursively( state );
		}
		if ( currentType.getSuperType( OptionalDouble.class ).isPresent() ) {
			state.append(
					OptionalDoubleValueExtractor.get(),
					state.introspector.getGenericTypeModel( Double.class )
			);
			return resolveContainerValueExtractorRecursively( state );
		}
		elementTypeModelOptional = currentType.getArrayElementType();
		if ( elementTypeModelOptional.isPresent() ) {
			state.append( ArrayValueExtractor.get(), elementTypeModelOptional.get() );
			return resolveContainerValueExtractorRecursively( state );
		}

		return state.buildOptional();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" }) // Checks are implemented using reflection
	private static class ExtractorResolutionState<C> {

		private final PojoIntrospector introspector;
		private ContainerValueExtractor<? super C, ?> extractor;
		private PojoGenericTypeModel<?> extractedType;

		ExtractorResolutionState(PojoIntrospector introspector, PojoGenericTypeModel<?> extractedType) {
			this.introspector = introspector;
			this.extractedType = extractedType;
		}

		<T> void append(ContainerValueExtractor<?, T> extractor, PojoGenericTypeModel<T> extractedType) {
			this.extractedType = extractedType;
			if ( this.extractor == null ) {
				// Initial calls: T == ? super C
				this.extractor = (ContainerValueExtractor) extractor;
			}
			else {
				this.extractor = new ChainingContainerValueExtractor( this.extractor, extractor );
			}
		}

		Optional<BoundContainerValueExtractor<? super C, ?>> buildOptional() {
			if ( extractor == null ) {
				return Optional.empty();
			}
			else {
				return Optional.of( new BoundContainerValueExtractor( extractor, extractedType ) );
			}
		}

	}
}
