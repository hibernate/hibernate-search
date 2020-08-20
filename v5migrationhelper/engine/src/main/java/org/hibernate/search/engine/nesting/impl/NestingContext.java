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
 * Allows backends to insert "marker" fields representing the current nesting in case of index embedded associations.
 * <p>
 * Experimental, non-exposed work-around to facilitate creating proper ES document structures until we'll have migrated
 * off using Lucene {@code Document} objects as the one and only document representation.
 *
 * @author Gunnar Morling
 */
public interface NestingContext {

	void push(EmbeddedTypeMetadata embeddedTypeMetadata);

	void pop();

	void mark(Document document);

	void incrementCollectionIndex();
}
