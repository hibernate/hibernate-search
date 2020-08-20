/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.readerprovider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import org.apache.lucene.index.StoredFieldVisitor.Status;
import org.hibernate.search.query.engine.impl.ReusableDocumentStoredFieldVisitor;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.search.testsupport.readerprovider.FieldSelectorLeakingReaderProvider.forgeFieldInfo;

/**
 * ReaderProvider to inspect the type of FieldSelector being applied.
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1738")
public class FieldSelectorTest {

	@Test
	public void testEagerStop() throws IOException {
		HashSet<String> acceptedFieldNames = new HashSet<>();
		acceptedFieldNames.add( "field one" );
		acceptedFieldNames.add( "field two" );
		ReusableDocumentStoredFieldVisitor fieldVisitor = new ReusableDocumentStoredFieldVisitor( acceptedFieldNames );
		Assert.assertEquals( Status.NO, fieldVisitor.needsField( forgeFieldInfo( "made up field one" ) ) );
		Assert.assertEquals( Status.YES, fieldVisitor.needsField( forgeFieldInfo( "field one" ) ) );
		consumeField( fieldVisitor );
		Assert.assertEquals( Status.NO, fieldVisitor.needsField( forgeFieldInfo( "made up field two" ) ) );
		Assert.assertEquals( Status.NO, fieldVisitor.needsField( forgeFieldInfo( "made up field three" ) ) );
		Assert.assertEquals( Status.YES, fieldVisitor.needsField( forgeFieldInfo( "field two" ) ) );
		consumeField( fieldVisitor );
		Assert.assertEquals( Status.STOP, fieldVisitor.needsField( forgeFieldInfo( "made up field four" ) ) );

		//Fetch Document, prepare for processing next one:
		fieldVisitor.getDocumentAndReset();
		Assert.assertEquals( Status.NO, fieldVisitor.needsField( forgeFieldInfo( "made up field one" ) ) );
		Assert.assertEquals( Status.YES, fieldVisitor.needsField( forgeFieldInfo( "field one" ) ) );
	}

	private void consumeField(ReusableDocumentStoredFieldVisitor fieldVisitor) throws IOException {
		fieldVisitor.stringField( forgeFieldInfo( "anything" ), "anything".getBytes( StandardCharsets.UTF_8 ) );
	}

}
