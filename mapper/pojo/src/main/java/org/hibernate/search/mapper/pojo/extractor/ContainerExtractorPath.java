/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * ContainerExtractorPath represents a list of container value extractor to be applied to a property.
 * <p>
 * The extractors are either represented:
 * <ul>
 * <li>explicitly by their classes, e.g. {@code [MapValuesExtractor.class, CollectionElementExtractor.class]},
 * meaning "apply an instance of MapValuesExtractor on the property value, then apply an instance of
 * CollectionElementExtractor on the map values".
 * <li>or simply by the "default" path ({@link #defaultExtractors()}),
 * which means "whatever default Hibernate Search manages to apply using its internal extractor resolution algorithm".
 * This second form may result in different "resolved" paths depending on the type of the property it is applied to.
 * </ul>
 */
public class ContainerExtractorPath {

	private static final ContainerExtractorPath DEFAULT = new ContainerExtractorPath(
			true, Collections.emptyList()
	);
	private static final ContainerExtractorPath NONE = new ContainerExtractorPath(
			false, Collections.emptyList()
	);

	public static ContainerExtractorPath defaultExtractors() {
		return DEFAULT;
	}

	public static ContainerExtractorPath noExtractors() {
		return NONE;
	}

	public static ContainerExtractorPath explicitExtractor(
			Class<? extends ContainerExtractor> extractorClass) {
		return new ContainerExtractorPath(
				false,
				Collections.singletonList( extractorClass )
		);
	}

	public static ContainerExtractorPath explicitExtractors(
			List<? extends Class<? extends ContainerExtractor>> extractorClasses) {
		if ( extractorClasses.isEmpty() ) {
			return noExtractors();
		}
		else {
			return new ContainerExtractorPath(
					false,
					Collections.unmodifiableList( new ArrayList<>( extractorClasses ) )
			);
		}
	}

	private final boolean applyDefaultExtractors;
	private final List<? extends Class<? extends ContainerExtractor>> explicitExtractorClasses;

	private ContainerExtractorPath(boolean applyDefaultExtractors,
			List<? extends Class<? extends ContainerExtractor>> explicitExtractorClasses) {
		this.applyDefaultExtractors = applyDefaultExtractors;
		this.explicitExtractorClasses = explicitExtractorClasses;
	}

	@Override
	public boolean equals(Object obj) {
		if ( ! ( obj instanceof ContainerExtractorPath ) ) {
			return false;
		}
		ContainerExtractorPath other = (ContainerExtractorPath) obj;
		return applyDefaultExtractors == other.applyDefaultExtractors
				&& Objects.equals( explicitExtractorClasses, other.explicitExtractorClasses );
	}

	@Override
	public int hashCode() {
		return Objects.hash( applyDefaultExtractors, explicitExtractorClasses );
	}

	@Override
	public String toString() {
		if ( isDefault() ) {
			return "<default value extractors>";
		}
		else if ( explicitExtractorClasses.isEmpty() ) {
			return "<no value extractors>";
		}
		else {
			StringJoiner joiner = new StringJoiner( ", ", "<", ">" );
			for ( Class<? extends ContainerExtractor> extractorClass : explicitExtractorClasses ) {
				joiner.add( extractorClass.getName() );
			}
			return joiner.toString();
		}
	}

	public boolean isDefault() {
		return applyDefaultExtractors;
	}

	public boolean isEmpty() {
		return !isDefault() && explicitExtractorClasses.isEmpty();
	}

	public List<? extends Class<? extends ContainerExtractor>> getExplicitExtractorClasses() {
		return explicitExtractorClasses;
	}
}
