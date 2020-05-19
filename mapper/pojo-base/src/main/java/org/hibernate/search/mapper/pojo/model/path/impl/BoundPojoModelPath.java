/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.binding.impl.PojoModelPathWalker;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * Represents an arbitrarily long access path bound to a specific POJO model.
 * <p>
 * This class and its various subclasses are similar to {@link PojoModelPath},
 * except they provide information about the types they are bound to.
 * As a result, they include type node. For instance the path could be:
 * <code>
 * Type A =&gt; property "propertyOfA" =&gt; extractor "MapValueExtractor" =&gt; Type B =&gt; property "propertyOfB"
 * </code>
 */
public abstract class BoundPojoModelPath {

	public static Walker walker(ContainerExtractorBinder containerExtractorBinder) {
		return new Walker( containerExtractorBinder );
	}

	public static <T> BoundPojoModelPathOriginalTypeNode<T> root(PojoTypeModel<T> typeModel) {
		return new BoundPojoModelPathOriginalTypeNode<>( null, typeModel );
	}

	BoundPojoModelPath() {
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

	public abstract BoundPojoModelPath getParent();

	public abstract PojoTypeModel<?> getRootType();

	public abstract PojoModelPath toUnboundPath();

	abstract void appendSelfPath(StringBuilder builder);

	private void appendPath(StringBuilder builder) {
		BoundPojoModelPath parent = getParent();
		if ( parent == null ) {
			appendSelfPath( builder );
		}
		else {
			parent.appendPath( builder );
			builder.append( " => " );
			appendSelfPath( builder );
		}
	}

	abstract void appendSelfPath(PojoModelPath.Builder builder);

	final void appendPath(PojoModelPath.Builder builder) {
		BoundPojoModelPath parent = getParent();
		if ( parent != null ) {
			parent.appendPath( builder );
		}
		appendSelfPath( builder );
	}

	public static class Walker implements PojoModelPathWalker<
			BoundPojoModelPathTypeNode<?>,
			BoundPojoModelPathPropertyNode<?, ?>,
			BoundPojoModelPathValueNode<?, ?, ?>
			> {

		private final ContainerExtractorBinder containerExtractorBinder;

		private Walker(ContainerExtractorBinder containerExtractorBinder) {
			this.containerExtractorBinder = containerExtractorBinder;
		}

		@Override
		public BoundPojoModelPathPropertyNode<?, ?> property(BoundPojoModelPathTypeNode<?> typeNode,
				String propertyName) {
			return typeNode.property( propertyName );
		}

		@Override
		public BoundPojoModelPathValueNode<?, ?, ?> value(BoundPojoModelPathPropertyNode<?, ?> propertyNode,
				ContainerExtractorPath extractorPath) {
			return doValue( propertyNode, extractorPath );
		}

		@Override
		public BoundPojoModelPathTypeNode<?> type(BoundPojoModelPathValueNode<?, ?, ?> valueNode) {
			return valueNode.type();
		}

		private <P> BoundPojoModelPathValueNode<?, P, ?> doValue(BoundPojoModelPathPropertyNode<?, P> propertyNode,
				ContainerExtractorPath extractorPath) {
			BoundContainerExtractorPath<P, ?> boundExtractorPath = containerExtractorBinder
					.bindPath( propertyNode.getPropertyModel().typeModel(), extractorPath );
			return propertyNode.value( boundExtractorPath );
		}
	}
}
