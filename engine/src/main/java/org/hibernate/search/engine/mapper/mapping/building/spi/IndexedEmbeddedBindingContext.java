/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Collection;
import java.util.Set;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;

/**
 * The binding context associated to a specific non-root node in the entity tree.
 *
 * @see IndexBindingContext
 */
public interface IndexedEmbeddedBindingContext extends IndexBindingContext {

	/**
	 * @return The list of index object fields between the parent binding context and this context.
	 */
	Collection<IndexObjectFieldReference> getParentIndexObjectReferences();

	/**
	 * @return The set of {@code includePaths} filters that did not match anything so far.
	 */
	Set<String> getUselessIncludePaths();

	/**
	 * @return The set of encountered field paths so far.
	 */
	Set<String> getEncounteredFieldPaths();

}
