/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Represents an arbitrarily long access path when walking the POJO model.
 * <p>
 * For instance the path could be:
 * <code>
 * property "propertyOfA" =&gt; extractor "MapValueExtractor" =&gt; property "propertyOfB"
 * </code>
 * Meaning: extract property "propertyOfA", then extract values using "MapValueExtractor",
 * then for each value extract property "propertyOfB".
 */
public abstract class PojoModelPath {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected static final Pattern DOT_PATTERN = Pattern.compile( "\\." );

	/**
	 * @return A builder allowing to create a {@link PojoModelPath}
	 * by specifying its components (property, container extractors) one by one.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * @param propertyName The name of a POJO property.
	 * @return A path from a POJO to the property with the given name.
	 */
	public static PojoModelPathPropertyNode ofProperty(String propertyName) {
		return new PojoModelPathPropertyNode( null, propertyName );
	}

	/**
	 * @param propertyName The name of a POJO property.
	 * @return A {@link PojoModelPath} from a POJO to the value(s) of the property with the given name.
	 * Default container extractors are applied to the property,
	 * so that for example a path to a List property will in fact point to the <em>elements</em> of that list.
	 */
	public static PojoModelPathValueNode ofValue(String propertyName) {
		return ofValue( propertyName, ContainerExtractorPath.defaultExtractors() );
	}

	/**
	 * @param propertyName The name of a POJO property.
	 * @param extractorPath A container extractor path.
	 * @return A {@link PojoModelPath} from a POJO to the value(s) of the property with the given name.
	 * The extractors represented by the given extractor path are applied to the property,
	 * so that for example a path to a List property may in fact point to the <em>elements</em> of that list.
	 */
	public static PojoModelPathValueNode ofValue(String propertyName, ContainerExtractorPath extractorPath) {
		return new PojoModelPathValueNode( ofProperty( propertyName ), extractorPath );
	}

	/**
	 * @param dotSeparatedPath A dot-separated path, such as {@code "myProperty.someNestedProperty"}.
	 * @return A {@link PojoModelPath} representing the same path,
	 * which the default container extractors applied to each property,
	 * so that for example a path to a List property will in fact point to the <em>elements</em> of that list.
	 */
	public static PojoModelPathValueNode parse(String dotSeparatedPath) {
		Contracts.assertNotNullNorEmpty( dotSeparatedPath, "dotSeparatedPath" );
		Builder builder = builder();
		for ( String propertyName : DOT_PATTERN.split( dotSeparatedPath, -1 ) ) {
			builder.property( propertyName ).valueWithDefaultExtractors();
		}
		return builder.toValuePath();
	}

	PojoModelPath() {
		// Package-protected constructor
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( getClass().getSimpleName() )
				.append( "[" );
		appendPath( builder );
		builder.append( "]" );
		return builder.toString();
	}

	/**
	 * @return A representation of this path in the form
	 * {@code propertyA<containerExtractorPathA>.propertyB<containerExtractorPathB>.propertyC<containerExtractorPathB>}.
	 */
	public final String toPathString() {
		StringBuilder builder = new StringBuilder();
		appendPath( builder );
		return builder.toString();
	}

	/**
	 * @return The model path to the element from which the value represented by this node is extracted.
	 * May be {@code null}.
	 */
	public abstract PojoModelPath parent();

	abstract void appendSelfPath(StringBuilder builder);

	private void appendPath(StringBuilder builder) {
		PojoModelPath parent = parent();
		if ( parent == null ) {
			appendSelfPath( builder );
		}
		else {
			parent.appendPath( builder );
			appendSelfPath( builder );
		}
	}

	@SuppressWarnings("rawtypes")
	public static class Builder {

		private PojoModelPathPropertyNode currentPropertyNode;
		private final List<String> currentExplicitExtractors = new ArrayList<>();
		private boolean noExtractors;
		private boolean defaultExtractors;

		private Builder() {
		}

		/**
		 * Append to the path an access the property with the given name.
		 *
		 * @param propertyName The name of the property to access.
		 * @return {@code this}, for method chaining.
		 * @throws org.hibernate.search.util.common.SearchException If no property name was previously given.
		 */
		public Builder property(String propertyName) {
			currentPropertyNode = new PojoModelPathPropertyNode(
					toValuePathOrNull(),
					propertyName
			);
			return this;
		}

		/**
		 * Append to the path a value extraction using the given container extractor path.
		 *
		 * @param extractorPath The container extractors to apply, as a {@link ContainerExtractorPath}.
		 * @return {@code this}, for method chaining.
		 * @throws org.hibernate.search.util.common.SearchException If no property name was previously given.
		 */
		public Builder value(ContainerExtractorPath extractorPath) {
			if ( extractorPath.isDefault() ) {
				return valueWithDefaultExtractors();
			}
			else if ( extractorPath.isEmpty() ) {
				return valueWithoutExtractors();
			}
			else {
				for ( String extractorName : extractorPath.explicitExtractorNames() ) {
					value( extractorName );
				}
				return this;
			}
		}

		/**
		 * Append to the path a value extraction using the given container extractor.
		 * <p>
		 * Multiple {@link #value(String)} calls can be chained to apply multiple extractors.
		 *
		 * @param extractorName The name of the container extractor to apply.
		 * @return {@code this}, for method chaining.
		 * @throws org.hibernate.search.util.common.SearchException If no property name was previously given.
		 * @see org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors
		 */
		public Builder value(String extractorName) {
			checkHasPropertyName();
			if ( defaultExtractors ) {
				throw log.cannotUseDefaultExtractorsInMultiExtractorChain();
			}
			noExtractors = false;
			currentExplicitExtractors.add( extractorName );
			return this;
		}

		/**
		 * Append to the path a direct value extraction, not using any container extractors.
		 * @return {@code this}, for method chaining.
		 * @throws org.hibernate.search.util.common.SearchException If no property name was previously given.
		 */
		public Builder valueWithoutExtractors() {
			checkHasPropertyName();
			noExtractors = true;
			return this;
		}

		/**
		 * Append to the path a value extraction using the default container extractors.
		 * @return {@code this}, for method chaining.
		 * @throws org.hibernate.search.util.common.SearchException If no property name was previously given.
		 */
		public Builder valueWithDefaultExtractors() {
			checkHasPropertyName();
			if ( !currentExplicitExtractors.isEmpty() ) {
				throw log.cannotUseDefaultExtractorsInMultiExtractorChain();
			}
			noExtractors = false;
			defaultExtractors = true;
			return this;
		}

		/**
		 * @return A {@link PojoModelPathPropertyNode} built from the given components.
		 * @throws org.hibernate.search.util.common.SearchException If no initial property name was given.
		 */
		public PojoModelPathPropertyNode toPropertyPath() {
			checkHasPropertyName();
			return currentPropertyNode;
		}

		/**
		 * @return A {@link PojoModelPathPropertyNode} built from the given components,
		 * or {@code null} if no information was added to this builder.
		 */
		public PojoModelPathPropertyNode toPropertyPathOrNull() {
			if ( isEmpty() ) {
				return null;
			}
			return toPropertyPath();
		}

		/**
		 * @return A {@link PojoModelPathValueNode} built from the given components.
		 * @throws org.hibernate.search.util.common.SearchException If no initial property name was given.
		 */
		public PojoModelPathValueNode toValuePath() {
			return new PojoModelPathValueNode( toPropertyPath(), flushContainerExtractorPath() );
		}

		/**
		 * @return A {@link PojoModelPathValueNode} built from the given components,
		 * or {@code null} if no information was added to this builder.
		 */
		public PojoModelPathValueNode toValuePathOrNull() {
			if ( isEmpty() ) {
				return null;
			}
			return toValuePath();
		}

		private boolean isEmpty() {
			// Empty if nothing was called
			return currentPropertyNode == null
					&& !noExtractors && !defaultExtractors && currentExplicitExtractors.isEmpty();
		}

		private ContainerExtractorPath flushContainerExtractorPath() {
			ContainerExtractorPath result;
			if ( !currentExplicitExtractors.isEmpty() ) {
				result = ContainerExtractorPath.explicitExtractors( currentExplicitExtractors );
			}
			else if ( noExtractors ) {
				result = ContainerExtractorPath.noExtractors();
			}
			else { // Default
				result = ContainerExtractorPath.defaultExtractors();
			}
			currentExplicitExtractors.clear();
			noExtractors = false;
			defaultExtractors = false;
			return result;
		}

		private void checkHasPropertyName() {
			if ( currentPropertyNode == null ) {
				throw log.cannotDefinePojoModelPathWithoutProperty();
			}
		}

	}
}
