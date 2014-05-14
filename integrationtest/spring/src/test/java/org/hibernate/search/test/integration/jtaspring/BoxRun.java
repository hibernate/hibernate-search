/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
