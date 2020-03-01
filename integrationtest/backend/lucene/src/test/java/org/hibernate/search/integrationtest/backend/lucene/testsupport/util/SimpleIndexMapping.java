/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;

public class SimpleIndexMapping {

	private final Map<String, RefType> references = new LinkedHashMap<>();

	public final <T> void add(String path, Class<T> type, IndexFieldReference<T> reference) {
		references.put( path, new RefType( type, reference, null ) );
	}

	public final void add(String path, ObjectFieldStorage storage, IndexObjectFieldReference reference) {
		references.put( path, new RefType( IndexObjectFieldReference.class, reference, storage ) );
	}

	public final Object getReference(String path) {
		RefType ref = references.get( path );
		if ( ref != null ) {
			return ref.reference;
		}
		return null;
	}

	public final IndexFieldReference getFieldReference(String path) {
		RefType ref = references.get( path );
		if ( ref != null ) {
			return (IndexFieldReference) ref.reference;
		}
		return null;
	}

	public final IndexObjectFieldReference getObjectReference(String path) {
		RefType ref = references.get( path );
		if ( ref != null ) {
			return (IndexObjectFieldReference) ref.reference;
		}
		return null;
	}

	public final ObjectFieldStorage getObjectStorage(String path) {
		RefType ref = references.get( path );
		if ( ref != null ) {
			return ref.storage;
		}
		return null;
	}

	public final Class<?> getType(String path) {
		RefType ref = references.get( path );
		if ( ref != null ) {
			return ref.type;
		}
		return null;
	}

	private static class RefType {

		Class<?> type;
		Object reference;
		ObjectFieldStorage storage;

		public RefType(Class<?> type, Object reference, ObjectFieldStorage storage) {
			this.type = type;
			this.reference = reference;
			this.storage = storage;
		}

	}

}
