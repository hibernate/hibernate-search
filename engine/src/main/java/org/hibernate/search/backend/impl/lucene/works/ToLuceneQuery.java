package org.hibernate.search.backend.impl.lucene.works;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.SerializableQuery;

/**
 * Interface to map the several SerializableQueries to a Lucene query.
 * 
 * This is done outside of the SerializableQuery class to prevent
 * incompatibilities during serialization because of Lucene API changes
 * 
 * @author Martin Braun
 */
interface ToLuceneQuery {

	public Query build(SerializableQuery serializableQuery);

}