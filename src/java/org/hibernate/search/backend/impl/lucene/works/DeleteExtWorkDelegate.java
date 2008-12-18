package org.hibernate.search.backend.impl.lucene.works;

import java.io.Serializable;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * Stateless extension of <code>DeleteLuceneWork</code>,
 * performing the same LuceneWork in an optimal way in case
 * the index is NOT shared across different entities
 * (which is the default).
 *
 * @author Sanne Grinovero
 * @see DeleteWorkDelegate
 */
public class DeleteExtWorkDelegate extends DeleteWorkDelegate {

	private final Class<?> managedType;
	private final DocumentBuilderIndexedEntity<?> builder;
	private final Logger log = LoggerFactory.make();

	DeleteExtWorkDelegate(Workspace workspace) {
		super( workspace );
		if ( workspace.getEntitiesInDirectory().size() != 1 ) {
			throw new AssertionFailure( "Can't use this delegate on shared indexes" );
		}
		managedType = workspace.getEntitiesInDirectory().iterator().next();
		builder = workspace.getDocumentBuilder( managedType );
	}

	@Override
	public void performWork(LuceneWork work, IndexWriter writer) {
		checkType( work );
		Serializable id = work.getId();
		log.trace( "Removing {}#{} by id using an IndexWriter.", managedType, id );
		Term idTerm = builder.getTerm( id );
		try {
			writer.deleteDocuments( idTerm );
		}
		catch ( Exception e ) {
			String message = "Unable to remove " + managedType + "#" + id + " from index.";
			throw new SearchException( message, e );
		}
	}

	private void checkType(final LuceneWork work) {
		if ( work.getEntityClass() != managedType ) {
			throw new AssertionFailure( "Unexpected type" );
		}
	}

}
