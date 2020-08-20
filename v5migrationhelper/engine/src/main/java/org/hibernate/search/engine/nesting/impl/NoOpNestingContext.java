/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.nesting.impl;

import org.apache.lucene.document.Document;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;

/**
 * No-op {@link NestingContext}.
 *
 * @author Gunnar Morling
 */
public class NoOpNestingContext implements NestingContext {

	public static final NestingContext INSTANCE = new NoOpNestingContext();

	@Override
	public void push(EmbeddedTypeMetadata embeddedTypeMetadata) {
	}

	@Override
	public void pop() {
	}

	@Override
	public void mark(Document document) {
	}

	@Override
	public void incrementCollectionIndex() {
	}
}
