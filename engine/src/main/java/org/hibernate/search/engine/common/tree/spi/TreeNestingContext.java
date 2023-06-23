/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.tree.spi;

import java.util.Optional;
import java.util.function.BiFunction;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.impl.ConfiguredTreeNestingContext;
import org.hibernate.search.engine.common.tree.impl.ExcludeAllTreeNestingContext;
import org.hibernate.search.engine.common.tree.impl.NotifyingTreeNestingContext;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.util.common.SearchException;

public interface TreeNestingContext {

	/**
	 * Nest a leaf node in this context.
	 * <p>
	 * The schema element will be created using the given factory,
	 * passing the {@link TreeNodeInclusion} to signal whether it's included or not.
	 * <p>
	 * The name passed to the factory will still be relative and still won't contain any dot ("."),
	 * but may be prefixed as required by this context's configuration.
	 *
	 * @param relativeName The base of the relative field name, which may get prefixed before it is passed to the factory.
	 * @param factory The element factory to use.
	 * @param <T> The type of the created node.
	 * @return The created node.
	 */
	<T> T nest(String relativeName, LeafFactory<T> factory);

	/**
	 * Nest a composite node in this context.
	 * <p>
	 * The node will be created using the given factory,
	 * passing the {@link TreeNodeInclusion} to signal whether it's included or not.
	 * <p>
	 * The name passed to the factory will still be relative and still won't contain any dot ("."),
	 * but may be prefixed as required by this context's configuration.
	 *
	 * @param relativeName The base of the relative field name, which may get prefixed before it is passed to the factory.
	 * @param factory The element factory to use.
	 * @param <T> The type of the created node.
	 * @return The created node.
	 */
	<T> T nest(String relativeName, CompositeFactory<T> factory);

	/**
	 * Nest an unfiltered node in this context.
	 * <p>
	 * The node will be created using the given factory,
	 * passing the {@link TreeNodeInclusion} to signal whether it's included or not.
	 * <p>
	 * Unfiltered nodes do not take inclusion filters into account;
	 * they are included as soon as their parent is included.
	 *
	 * @param factory The element factory to use.
	 * @param <T> The type of the created nodes.
	 * @return The created nodes.
	 */
	<T> T nestUnfiltered(UnfilteredFactory<T> factory);

	/**
	 * Creates a nested context within this context, composing the given filter within this context's filter.
	 * <p>
	 * The node will be created using the given {@code builder}.
	 * <p>
	 * If the resulting context excludes everything, this method returns {@link Optional#empty()}.
	 *
	 * @param mappingElement A unique representation of the mapping element defining the filter;
	 * if the same mapping is applied in multiple places,
	 * this method must be called with mapping elements that are equal according to {@link MappingElement#equals(Object)}/{@link MappingElement#hashCode()}.
	 * @param relativePrefix The prefix to prepend to the relative path of all nodes nested in the resulting context.
	 * @param definition The filter definition (included paths, ...).
	 * @param pathTracker The path tracker, for detection of useless filters.
	 * @param contextBuilder The builder for the created context.
	 * @param cyclicRecursionExceptionFactory A factory for exceptions thrown
	 * when encountering cyclic (infinite) filter recursions.
	 * @param <T> The type of the created context.
	 * @return The created context.
	 */
	<T> Optional<T> nestComposed(MappingElement mappingElement, String relativePrefix,
			TreeFilterDefinition definition,
			TreeFilterPathTracker pathTracker, NestedContextBuilder<T> contextBuilder,
			BiFunction<MappingElement, String, SearchException> cyclicRecursionExceptionFactory);

	static TreeNestingContext root() {
		return ConfiguredTreeNestingContext.ROOT;
	}

	/**
	 * @param delegate The nesting context to wrap.
	 * @param listener The listener to notify when nodes are contributed through the returned context.
	 * @return A context that notifies the given listener when nodes are contributed to (included in) it.
	 */
	static TreeNestingContext notifying(TreeNestingContext delegate, TreeContributionListener listener) {
		return new NotifyingTreeNestingContext( delegate, listener );
	}

	/**
	 * @return A nesting context that always excludes all elements and does not prefix the field names.
	 */
	static TreeNestingContext excludeAll() {
		return ExcludeAllTreeNestingContext.INSTANCE;
	}

	interface LeafFactory<T> {
		T create(String prefixedRelativeName, TreeNodeInclusion inclusion);
	}

	interface CompositeFactory<T> {
		T create(String prefixedRelativeName, TreeNodeInclusion inclusion,
				TreeNestingContext nestedNestingContext);
	}

	interface UnfilteredFactory<T> {
		T create(TreeNodeInclusion inclusion, String prefix);
	}

	interface NestedContextBuilder<T> {

		void appendObject(String objectName);

		T build(TreeNestingContext nestingContext);

	}

}
