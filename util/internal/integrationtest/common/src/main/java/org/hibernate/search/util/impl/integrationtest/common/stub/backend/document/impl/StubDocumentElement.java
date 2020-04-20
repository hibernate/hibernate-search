/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.spi.NoOpDocumentElement;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;

public class StubDocumentElement implements DocumentElement {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final StubDocumentNode.Builder builder;

	public StubDocumentElement(StubDocumentNode.Builder builder) {
		this.builder = builder;
	}

	@Override
	public <F> void addValue(IndexFieldReference<F> fieldReference, F value) {
		StubIndexFieldReference<F> stubFieldReference = (StubIndexFieldReference<F>) fieldReference;
		if ( !stubFieldReference.isEnabled() ) {
			log.tracev(
					"Ignoring write on document element {}, field '{}' with value '{}'" +
							" because the field was excluded during bootstrap.",
					this, stubFieldReference.getAbsolutePath(), value
			);
			return;
		}
		addValue( stubFieldReference.getRelativeFieldName(), value );
	}

	@Override
	public DocumentElement addObject(IndexObjectFieldReference fieldReference) {
		StubIndexObjectFieldReference stubFieldReference = (StubIndexObjectFieldReference) fieldReference;
		if ( !stubFieldReference.isEnabled() ) {
			log.tracev(
					"Ignoring add on document element {}, object field '{}'" +
							" because the field was excluded during bootstrap.",
					this, stubFieldReference.getAbsolutePath()
			);
			return NoOpDocumentElement.get();
		}
		return addObject( stubFieldReference.getRelativeFieldName() );
	}

	@Override
	public void addNullObject(IndexObjectFieldReference fieldReference) {
		StubIndexObjectFieldReference stubFieldReference = (StubIndexObjectFieldReference) fieldReference;
		if ( !stubFieldReference.isEnabled() ) {
			log.tracev(
					"Ignoring add missing on document element {}, object field '{}'" +
							" because the field was excluded during bootstrap.",
					this, stubFieldReference.getAbsolutePath()
			);
		}
		addNullObject( stubFieldReference.getRelativeFieldName() );
	}

	@Override
	public void addValue(String relativeFieldName, Object value) {
		builder.field( relativeFieldName, value );
	}

	@Override
	public DocumentElement addObject(String relativeFieldName) {
		StubDocumentNode.Builder childBuilder = StubDocumentNode.object( builder, relativeFieldName );
		builder.child( childBuilder );
		return new StubDocumentElement( childBuilder );
	}

	@Override
	public void addNullObject(String relativeFieldName) {
		builder.missingObjectField( relativeFieldName );
	}

}
