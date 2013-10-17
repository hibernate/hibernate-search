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
package org.hibernate.search.test.util;

import org.junit.Test;

import junit.framework.Assert;

/**
 * To be run in an IDE this test requires option -Dhibernate.service.allow_crawling=false
 * In fact the whole purpose is to verify the testsuite is run with this option enabled.
 *
 * We run the testsuite with ALLOW_CRAWLING disabled to make sure the project is using the
 * latest version (definition) of any Service defined by Hibernate ORM: since HHH-8619
 * ORM will attempt to translate legacy service requests remapping them to their new
 * version. By disabling this in our testsuite we make sure that at least at release time
 * of a version of Hibernate Search the services are up to date.
 *
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-1444")
public class EnvironmentTest {

	//TODO replace with constant org.hibernate.service.internal.AbstractServiceRegistryImpl#ALLOW_CRAWLING
	private static final String ALLOW_CRAWLING = "hibernate.service.allow_crawling";

	@Test
	public void hibernateORMServiceCrawlingDisabled() {
		String property = System.getProperty( ALLOW_CRAWLING );
		Assert.assertEquals( "false", property );
	}

}
