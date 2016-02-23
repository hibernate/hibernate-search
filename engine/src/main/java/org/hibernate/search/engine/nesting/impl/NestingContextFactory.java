/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.nesting.impl;

/**
 * Factory for creating {@link NestingContext}s.
 *
 * @author Gunnar Morling
 */
public interface NestingContextFactory {

	NestingContext createNestingContext(Class<?> indexedEntityType);
}
