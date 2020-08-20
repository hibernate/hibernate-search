/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import java.io.Serializable;

import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * @author Emmanuel Bernard
 */
public class DeleteLuceneWork extends LuceneWork {

	public DeleteLuceneWork(Serializable id, String idInString, IndexedTypeIdentifier typeIdentifier) {
		this( null, id, idInString, typeIdentifier );
	}

	public DeleteLuceneWork(String tenantId, Serializable id, String idInString, IndexedTypeIdentifier typeIdentifier) {
		super( tenantId, id, idInString, typeIdentifier );
	}

	@Override
	public <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p) {
		return visitor.visitDeleteWork( this, p );
	}

	@Override
	public String toString() {
		String tenant = getTenantId() == null ? "" : " [" + getTenantId() + "] ";
		return "DeleteLuceneWork" + tenant + ": " + this.getEntityType().getName() + "#" + this.getIdInString();
	}

}
