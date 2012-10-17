/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.infinispan.indexmanager;

import static org.hibernate.search.infinispan.ClusterTestHelper.createClusterNode;

import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.test.util.TestForIssue;

/**
 * Use these options to verify this test from your IDE:
 * -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1 -XX:MaxPermSize=256m
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-882")
public class IndexManagerClusteredTest extends BaseLiveClusterTest {

	protected FullTextSessionBuilder createNewNode() {
		return createClusterNode( entityTypes, true, false, true );
	}

}
