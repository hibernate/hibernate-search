//$Id$
package org.hibernate.search;

import org.hibernate.search.util.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class Version {
	
	public static final String VERSION = "3.2.0-SNAPSHOT";

	static {
		LoggerFactory.make().info( "Hibernate Search {}", VERSION );
	}

	public static void touch() {
	}
	
}
