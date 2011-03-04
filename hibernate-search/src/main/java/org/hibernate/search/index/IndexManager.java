/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.hibernate.search.index;

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.spi.BuildContext;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 *
 */
public interface IndexManager {
	
	/* lifecycle */
	
	public String getName();
	void initialize(String name, Properties properties, BuildContext context);
	void start();
	void stop();
	
	/* maintenance methods */
	
	public void create(boolean force);
	public void forceReleaseLocks() throws IOException;
	
	/* purpose */
	
	public void optimize();
	
	public IndexReader getIndexReader();
	public void releaseIndexReader(IndexReader ir);
	
	public IndexWriter getIndexWriter() throws IOException;
	public void releaseIndexWriter(IndexWriter ir);
	
}
