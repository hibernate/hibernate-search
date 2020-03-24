/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

public abstract class AbstractSingleDocumentIndexingWork extends AbstractIndexingWork<Long>
		implements SingleDocumentIndexingWork {

	protected final String tenantId;
	protected final String entityTypeName;
	protected final Object entityIdentifier;
	protected final String documentIdentifier;

	AbstractSingleDocumentIndexingWork(String workType, String tenantId,
			String entityTypeName, Object entityIdentifier, String documentIdentifier) {
		super( workType );
		this.tenantId = tenantId;
		this.entityTypeName = entityTypeName;
		this.entityIdentifier = entityIdentifier;
		this.documentIdentifier = documentIdentifier;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( workType )
				.append( ", tenantId=" ).append( tenantId )
				.append( ", entityTypeName=" ).append( entityTypeName )
				.append( ", entityIdentifier=" ).append( entityIdentifier )
				.append( "]" );
		return sb.toString();
	}

	@Override
	public String getEntityTypeName() {
		return entityTypeName;
	}

	@Override
	public Object getEntityIdentifier() {
		return entityIdentifier;
	}

	@Override
	public String getQueuingKey() {
		return documentIdentifier;
	}
}
