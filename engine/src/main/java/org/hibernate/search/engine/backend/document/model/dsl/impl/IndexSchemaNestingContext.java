/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl.impl;


import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;

public interface IndexSchemaNestingContext {

	/**
	 * Nest a leaf schema element in this context.
	 * <p>
	 * The schema element will be created using one of the two given factories,
	 * depending on whether internal filters lead to its inclusion or exclusion.
	 * <p>
	 * The name passed to the factory will still be relative and still won't contain any dot ("."),
	 * but may be prefixed as required by this context's configuration.
	 *
	 * @param relativeName The base of the relative field name, which may get prefixed before it is passed to the factory.
	 * @param factory The element factory to use.
	 * @param <T> The type of the created schema element.
	 * @return The created schema element.
	 */
	<T> T nest(String relativeName, LeafFactory<T> factory);

	/**
	 * Nest a composite schema element in this context.
	 * <p>
	 * The schema element will be created using one of the two given factories,
	 * depending on whether internal filters lead to its inclusion or exclusion.
	 * <p>
	 * The name passed to the factory will still be relative and still won't contain any dot ("."),
	 * but may be prefixed as required by this context's configuration.
	 *
	 * @param relativeName The base of the relative field name, which may get prefixed before it is passed to the factory.
	 * @param factory The element factory to use.
	 * @param <T> The type of the created schema element.
	 * @return The created schema element.
	 */
	<T> T nest(String relativeName, CompositeFactory<T> factory);

	/**
	 * Nest a template schema element in this context.
	 * <p>
	 * The schema element will be created using one of the two given factories,
	 * depending on whether it is included or excluded.
	 * Template elements do not take inclusion filters into account;
	 * they are included as soon as their parent is included.
	 *
	 * @param factory The element factory to use.
	 * @param <T> The type of the created schema element.
	 * @return The created schema element.
	 */
	<T> T nestTemplate(TemplateFactory<T> factory);

	/**
	 * @return A nesting context that always excludes all elements and does not prefix the field names.
	 */
	static IndexSchemaNestingContext excludeAll() {
		return ExcludeAllIndexSchemaNestingContext.INSTANCE;
	}

	interface LeafFactory<T> {
		T create(String prefixedRelativeName, IndexFieldInclusion inclusion);
	}

	interface CompositeFactory<T> {
		T create(String prefixedRelativeName, IndexFieldInclusion inclusion,
				IndexSchemaNestingContext nestedNestingContext);
	}

	interface TemplateFactory<T> {
		T create(IndexFieldInclusion inclusion, String prefix);
	}

}
