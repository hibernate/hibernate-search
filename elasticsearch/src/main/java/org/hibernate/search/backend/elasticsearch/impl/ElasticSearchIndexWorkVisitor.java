/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import io.searchbox.core.Delete;
import io.searchbox.core.Index;
import io.searchbox.params.Parameters;

import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.EntityIndexBinding;

import com.google.gson.JsonObject;

import static org.hibernate.search.backend.elasticsearch.client.impl.RequestHelper.executeRequest;

/**
 * @author Gunnar Morling
 *
 */
class ElasticSearchIndexWorkVisitor implements IndexWorkVisitor<Void, Void> {

	private static final Pattern DOT = Pattern.compile( "\\." );

	private final String indexName;
	private final ExtendedSearchIntegrator searchIntegrator;

	public ElasticSearchIndexWorkVisitor(String indexName, ExtendedSearchIntegrator searchIntegrator) {
		this.indexName = indexName;
		this.searchIntegrator = searchIntegrator;
	}

	@Override
	public Void visitAddWork(AddLuceneWork work, Void p) {
		indexDocument( work.getIdInString(), work.getDocument(), work.getEntityClass() );
		return null;
	}

	@Override
	public Void visitDeleteWork(DeleteLuceneWork work, Void p) {
		Delete delete = new Delete.Builder( work.getIdInString() )
			.index( indexName )
			.type( work.getEntityClass().getName() )
			// TODO Make configurable?
			.setParameter( Parameters.REFRESH, true )
			.build();

		executeRequest( delete );

		return null;
	}

	@Override
	public Void visitOptimizeWork(OptimizeLuceneWork work, Void p) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public Void visitPurgeAllWork(PurgeAllLuceneWork work, Void p) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public Void visitUpdateWork(UpdateLuceneWork work, Void p) {
		indexDocument( work.getIdInString(), work.getDocument(), work.getEntityClass() );
		return null;
	}

	@Override
	public Void visitFlushWork(FlushLuceneWork work, Void p) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public Void visitDeleteByQueryWork(
			DeleteByQueryLuceneWork work, Void p) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}


	private void indexDocument(String id, Document document, Class<?> entityType) {
		JsonObject source = convertToJson( document, entityType );
		String type = entityType.getName();

		Index index = new Index.Builder( source )
			.index( indexName )
			.type( type )
			.id( id )
			// TODO Make configurable?
			.setParameter( Parameters.REFRESH, true )
			.build();

		executeRequest( index );
	}

	private JsonObject convertToJson(Document document, Class<?> entityType) {
		EntityIndexBinding indexBinding = searchIntegrator.getIndexBinding( entityType );
		JsonObject source = new JsonObject();

		for ( IndexableField field : document.getFields() ) {
			if ( !field.name().equals( ProjectionConstants.OBJECT_CLASS ) &&
					!field.name().equals( indexBinding.getDocumentBuilder().getIdentifierName() ) ) {

				JsonObject parent = getOrCreateDocumentTree( source, field );
				String jsonPropertyName = field.name().substring( field.name().lastIndexOf( "." ) + 1 );

				if ( indexBinding.getDocumentBuilder().getTypeMetadata().getDocumentFieldMetadataFor( field.name() ).isNumeric() ) {
					parent.addProperty( jsonPropertyName, field.numericValue() );
				}
				else {
					parent.addProperty( jsonPropertyName, field.stringValue() );
				}
			}
		}

		return source;
	}

	private JsonObject getOrCreateDocumentTree(JsonObject source, IndexableField field) {
		// top-level property
		if ( !field.name().contains( "." ) ) {
			return source;
		}

		// embedded property Create JSON hierarchy as needed
		String[] parts = DOT.split( field.name() );
		JsonObject parent = source;

		for ( int i = 0; i < parts.length - 1; i++ ) {
			JsonObject newParent = parent.getAsJsonObject( parts[i] );
			if ( newParent == null ) {
				newParent = new JsonObject();
				parent.add( parts[i], newParent );
			}
			parent = newParent;
		}

		return parent;
	}
}