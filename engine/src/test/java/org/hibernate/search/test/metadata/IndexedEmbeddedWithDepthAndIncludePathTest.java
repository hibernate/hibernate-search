/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.metadata;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.TestForIssue;

import static org.junit.Assert.assertNotNull;

@TestForIssue(jiraKey = "HSEARCH-1442")
public class IndexedEmbeddedWithDepthAndIncludePathTest {

	private AnnotationMetadataProvider metadataProvider;

	@Before
	public void setUp() {
		ConfigContext configContext = new ConfigContext( new ManualConfiguration() );
		metadataProvider = new AnnotationMetadataProvider( new JavaReflectionManager(), configContext );
	}

	@Test
	public void testDepthIsProperlyHandled() {
		TypeMetadata rootTypeMetadata = metadataProvider
				.getTypeMetadataFor( IndexedEmbeddedTestEntity.class );

		EmbeddedTypeMetadata embeddedWithDepthTypeMetadata = null;
		for ( EmbeddedTypeMetadata typeMetadata : rootTypeMetadata.getEmbeddedTypeMetadata() ) {
			if ( "indexedEmbeddedWithDepth".equals( typeMetadata.getEmbeddedFieldName() ) ) {
				embeddedWithDepthTypeMetadata = typeMetadata;
			}
		}

		assertNotNull( embeddedWithDepthTypeMetadata );
		assertNotNull( embeddedWithDepthTypeMetadata.getPropertyMetadataForProperty( "name" ) );
	}

}
