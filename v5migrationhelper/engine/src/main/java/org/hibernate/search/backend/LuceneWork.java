/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import java.io.Serializable;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * Represent a unit of work to be applied against the Lucene index.
 *
 * <p>
 * Note:<br>
 * Instances of this class are passed between Virtual Machines when a master/slave
 * configuration of Search is used. It is the responsibility of the {@code LuceneWorkSerializer} respectively
 * {@code SerializationProvider} to serialize and de-serialize {@code LuceneWork} instances.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public abstract class LuceneWork {

	private final Document document;
	private final IndexedTypeIdentifier entityType;
	private final String tenantId;
	private final Serializable id;
	private final String idInString;

	public LuceneWork(String tenantId, Serializable id, String idInString, IndexedTypeIdentifier typeIdentifier) {
		this( tenantId, id, idInString, typeIdentifier, null );
	}

	public LuceneWork(String tenantId, Serializable id, String idInString, IndexedTypeIdentifier typeIdentifier, Document document) {
		this.tenantId = tenantId;
		this.id = id;
		this.idInString = idInString;
		this.entityType = typeIdentifier;
		this.document = document;
	}

	public Document getDocument() {
		return document;
	}

	/**
	 * @deprecated use {@link #getEntityType()}: this method will be removed!
	 * @return the Class identifying the entity type
	 */
	@Deprecated
	public Class<?> getEntityClass() {
		return entityType.getPojoType();
	}

	public IndexedTypeIdentifier getEntityType() {
		return entityType;
	}

	public Serializable getId() {
		return id;
	}

	public String getIdInString() {
		return idInString;
	}

	public String getTenantId() {
		return tenantId;
	}

	/**
	 * Accepts the given visitor by dispatching the correct visit method for the specific {@link LuceneWork} sub-type.
	 *
	 * @param <P> Context parameter type expected by a specific visitor
	 * @param <R> Return type provided by a specific visitor
	 * @param visitor the visitor to accept
	 * @param p a visitor-specific context parameter
	 * @return a visitor-specific return value or {@code null} if this visitor doesn't return a result
	 */
	public abstract <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p);

	public Map<String, String> getFieldToAnalyzerMap() {
		return null;
	}
}
