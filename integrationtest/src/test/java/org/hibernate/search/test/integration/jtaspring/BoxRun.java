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
package org.hibernate.search.test.integration.jtaspring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BoxRun {

	private ApplicationContext applicationContext = new ClassPathXmlApplicationContext( "classpath:beans.xml" );

	public void run() {
		BoxDAO boxDAO = (BoxDAO) applicationContext.getBean( "boxDAO" );

		Box box = new Box();
		box.setColor( "red-and-white" );
		boxDAO.persist( box );

		Muffin muffin = new Muffin();
		muffin.setKind( "blueberry" );
		muffin.setBox( box );

		box.addMuffin( muffin );

		boxDAO.merge( box );
	}

	public static void main(String args[]) {
		new BoxRun().run();
	}
}
