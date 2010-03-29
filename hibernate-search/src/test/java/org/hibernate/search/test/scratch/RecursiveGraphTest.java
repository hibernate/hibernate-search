/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 * 
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.scratch;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.hibernate.Session;
import org.hibernate.search.test.SearchTestCase;

public class RecursiveGraphTest extends SearchTestCase {

	public void testCreateParentAndChild() throws Exception {
        Person[] people = new Person[2];
        Person parent = new Person();
        parent.setName( "parent" );
        Person child = new Person();
        child.setName( "child" );
        connectChildToParent(child, parent);
        people[0] = parent;
        people[1] = child;
        savePeople( people );
        showIndexContentsForType( Person.class );
        assertEquals( 2, getDocumentNbr( Person.class ) );
    }
	
	private void connectChildToParent(Person child, Person parent) {
        Event birthEvent = child.getBirthEvent();
        child.setBirthEvent(birthEvent);
        ParentOfBirthEvent parentOfBirthEvent = new ParentOfBirthEvent(parent, child.getBirthEvent());
        parent.getParentOfBirthEvents().add(parentOfBirthEvent);
    }
	
	public void savePeople(Person... people) {
        for (Person person : people) {
        	if (person==null) continue;
        	Session s = getSessions().openSession();
    		s.getTransaction().begin();
    		s.save( person );
    		s.getTransaction().commit();
    		s.close();
        }
    }

	@Override
	protected Class<?>[] getMappings() {
		return new Class[]{
				Event.class,
				Person.class,
				ParentOfBirthEvent.class
		};
	}
	
	private int getDocumentNbr(Class type) throws Exception {
		IndexReader reader = IndexReader.open( getDirectory( type ), false );
		try {
			return reader.numDocs();
		}
		finally {
			reader.close();
		}
	}
	
	private void showIndexContentsForType(Class type) throws CorruptIndexException, IOException {
		IndexSearcher searcher = new IndexSearcher( getDirectory( type ), false );
		try {
			Query q = new MatchAllDocsQuery();
			TopDocs docs = searcher.search( q, null, 100 );
			ScoreDoc[] scoreDocs = docs.scoreDocs;
			for (ScoreDoc sd : scoreDocs) {
				Document doc = searcher.doc( sd.doc );
				StringBuilder sb = new StringBuilder();
				for (Fieldable field : (List<Fieldable>)doc.getFields()) {
					sb.append( field.name() ).append( ":" ).append( field.stringValue() ).append( " " );
				}
				System.out.println( sb.toString() );
			}
		}
		finally {
			searcher.close();
		}
	}

}
