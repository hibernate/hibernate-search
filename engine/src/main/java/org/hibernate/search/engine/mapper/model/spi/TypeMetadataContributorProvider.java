/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.model.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A provider of type metadata contributors, taking into account explicit contributions,
 * implicit discovery and inheritance.
 *
 * @param <C> The Java type of type metadata contributors.
 */
public final class TypeMetadataContributorProvider<C> {

	public static <C> Builder<C> builder() {
		return new Builder<>();
	}

	private final Map<MappableTypeModel, List<C>> contributionByType;
	private final List<TypeMetadataDiscoverer<C>> metadataDiscoverers;
	private final Set<MappableTypeModel> typesSubmittedToDiscoverers = new HashSet<>();

	private TypeMetadataContributorProvider(Builder<C> builder) {
		this.contributionByType = builder.contributionByType;
		this.metadataDiscoverers = builder.metadataDiscoverers;
	}

	/**
	 * @param typeModel The model of a type to retrieve contributors for, including supertype contributors.
	 *
	 * @return A set of metadata contributors
	 */
	public Set<C> get(MappableTypeModel typeModel) {
		return typeModel.descendingSuperTypes()
				.map( this::getContributionIncludingAutomaticallyDiscovered )
				.filter( Objects::nonNull )
				.flatMap( List::stream )
				// Using a LinkedHashSet because the order matters.
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	/**
	 * @param typeModel The model of a type to retrieve contributors for, excluding supertype contributors.
	 *
	 * @return A set of metadata contributors
	 */
	public Set<C> getIgnoringInheritance(MappableTypeModel typeModel) {
		List<C> result = getContributionIncludingAutomaticallyDiscovered( typeModel );
		if ( result == null || result.isEmpty() ) {
			return Collections.emptySet();
		}
		else {
			// Using a LinkedHashSet because the order matters.
			return new LinkedHashSet<>( result );
		}
	}

	/**
	 * @return A set containing all the types that were contributed to so far.
	 */
	public Set<? extends MappableTypeModel> typesContributedTo() {
		// Use a LinkedHashSet for deterministic iteration
		return Collections.unmodifiableSet( new LinkedHashSet<>( contributionByType.keySet() ) );
	}

	private List<C> getContributionIncludingAutomaticallyDiscovered(
			MappableTypeModel typeModel) {
		if ( !typesSubmittedToDiscoverers.contains( typeModel ) ) {
			// Allow automatic discovery of metadata the first time we encounter each type
			for ( TypeMetadataDiscoverer<C> metadataDiscoverer : metadataDiscoverers ) {
				Optional<C> discoveredContributor = metadataDiscoverer.discover( typeModel );
				if ( discoveredContributor.isPresent() ) {
					contributionByType.computeIfAbsent( typeModel, ignored -> new ArrayList<>() )
							.add( discoveredContributor.get() );
				}
			}
			typesSubmittedToDiscoverers.add( typeModel );
		}
		return contributionByType.get( typeModel );
	}

	public static final class Builder<C> {
		// Use a LinkedHashMap for deterministic iteration
		private final Map<MappableTypeModel, List<C>> contributionByType = new LinkedHashMap<>();
		private final List<TypeMetadataDiscoverer<C>> metadataDiscoverers = new ArrayList<>();

		private Builder() {
		}

		public void contributor(MappableTypeModel typeModel, C contributor) {
			contributionByType.computeIfAbsent( typeModel, ignored -> new ArrayList<>() )
					.add( contributor );
		}

		public void discoverer(TypeMetadataDiscoverer<C> metadataDiscoverer) {
			metadataDiscoverers.add( metadataDiscoverer );
		}

		public TypeMetadataContributorProvider<C> build() {
			return new TypeMetadataContributorProvider<>( this );
		}
	}
}
