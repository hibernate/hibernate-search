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

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractor;

/**
 * ContainerExtractorPath represents a list of container extractors to be applied to a property.
 * <p>
 * Container extractors tell Hibernate Search how to extract values:
 * an empty container extractor path means the property value should be taken as is,
 * a collection element extractor would extract each element of a collection,
 * a map keys extractor would extract each key of a map, etc.
 * <p>
 * Container extractor paths can chain multiple extractors,
 * so that for example the extraction of values from a {@code List<List<String>>} can be represented.
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
@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
public class ContainerExtractorPath {

	private static final ContainerExtractorPath DEFAULT = new ContainerExtractorPath(
			true, Collections.emptyList()
	);
	private static final ContainerExtractorPath NONE = new ContainerExtractorPath(
			false, Collections.emptyList()
	);

	/**
	 * @return A path that will apply the default extractor(s) based on the property type.
	 */
	public static ContainerExtractorPath defaultExtractors() {
		return DEFAULT;
	}

	/**
	 * @return A path that will not apply any container extractor.
	 */
	public static ContainerExtractorPath noExtractors() {
		return NONE;
	}

	/**
	 * @param extractorClass A container extractor referenced by its type.
	 * @return A path that will apply the referenced container extractor.
	 */
	public static ContainerExtractorPath explicitExtractor(
			Class<? extends ContainerExtractor> extractorClass) {
		return new ContainerExtractorPath(
				false,
				Collections.singletonList( extractorClass )
		);
	}

	/**
	 * @param extractorType A container extractor referenced by its type.
	 * @return A path that will apply the referenced container extractor.
	 */
	public static ContainerExtractorPath explicitExtractor(BuiltinContainerExtractor extractorType) {
		return explicitExtractor( extractorType.getType() );
	}

	/**
	 * @param extractorClasses A list of container extractors referenced by their type.
	 * @return A path that will apply the referenced container extractors in order.
	 */
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
