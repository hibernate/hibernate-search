/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;

/**
 * ContainerValueExtractorPath represents a list of container value extractor to be applied to a property.
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
 * <p>
 *
 */
public class ContainerValueExtractorPath {

	private static final ContainerValueExtractorPath DEFAULT = new ContainerValueExtractorPath(
			true, Collections.emptyList()
	);
	private static final ContainerValueExtractorPath NONE = new ContainerValueExtractorPath(
			false, Collections.emptyList()
	);

	public static ContainerValueExtractorPath defaultExtractors() {
		return DEFAULT;
	}

	public static ContainerValueExtractorPath noExtractors() {
		return NONE;
	}

	public static ContainerValueExtractorPath explicitExtractors(
			List<? extends Class<? extends ContainerValueExtractor>> extractorClasses) {
		if ( extractorClasses.isEmpty() ) {
			return noExtractors();
		}
		else {
			return new ContainerValueExtractorPath(
					false,
					Collections.unmodifiableList( new ArrayList<>( extractorClasses ) )
			);
		}
	}

	private final boolean applyDefaultExtractors;
	private final List<? extends Class<? extends ContainerValueExtractor>> explicitExtractorClasses;

	private ContainerValueExtractorPath(boolean applyDefaultExtractors,
			List<? extends Class<? extends ContainerValueExtractor>> explicitExtractorClasses) {
		this.applyDefaultExtractors = applyDefaultExtractors;
		this.explicitExtractorClasses = explicitExtractorClasses;
	}

	@Override
	public boolean equals(Object obj) {
		if ( ! ( obj instanceof ContainerValueExtractorPath ) ) {
			return false;
		}
		ContainerValueExtractorPath other = (ContainerValueExtractorPath) obj;
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
			for ( Class<? extends ContainerValueExtractor> extractorClass : explicitExtractorClasses ) {
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

	public List<? extends Class<? extends ContainerValueExtractor>> getExplicitExtractorClasses() {
		return explicitExtractorClasses;
	}
}
