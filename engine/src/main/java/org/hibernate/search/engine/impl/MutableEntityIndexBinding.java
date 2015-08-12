/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;

/**
 * @author Emmanuel Bernard
 */
public interface MutableEntityIndexBinding extends EntityIndexBinding {
	/**
	 * Allows to set the document builder for this {@code EntityIndexBinding}.
	 *
	 * @param documentBuilder the new document builder instance
	 */
	void setDocumentBuilderIndexedEntity(DocumentBuilderIndexedEntity documentBuilder);
}
