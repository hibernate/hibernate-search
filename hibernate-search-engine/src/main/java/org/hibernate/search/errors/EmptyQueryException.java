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
package org.hibernate.search.errors;

import org.hibernate.search.SearchException;


/**
 * This Exception is thrown when an empty TermQuery (keyword query) is created,
 * or if any string query only returns whitespace after applying Analyzers.
 *
 * Applications should validate user input before running such a Query;
 *
 * @see org.hibernate.search.util.AnalyzerUtils
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class EmptyQueryException extends SearchException {

	public EmptyQueryException() {
		super();
	}

	public EmptyQueryException(String message) {
		super( message );
	}

	public EmptyQueryException(String message, Throwable cause) {
		super( message, cause );
	}

	public EmptyQueryException(Throwable cause) {
		super( cause );
	}

}
