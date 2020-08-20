/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.nesting.impl;

import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * Always returns the no-op context.
 *
 * @author Gunnar Morling
 */
public class DefaultNestingContextFactory implements NestingContextFactory {

	public static final NestingContextFactory INSTANCE = new DefaultNestingContextFactory();

	@Override
	public NestingContext createNestingContext(IndexedTypeIdentifier indexedEntityType) {
		return NoOpNestingContext.INSTANCE;
	}

}
