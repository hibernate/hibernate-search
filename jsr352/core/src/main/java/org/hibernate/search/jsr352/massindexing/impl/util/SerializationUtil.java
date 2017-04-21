/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.hibernate.search.util.StringHelper;
import org.jboss.logging.Logger;

/**
 * @author Mincong Huang
 */
public final class SerializationUtil {

	public static final Logger LOGGER = Logger.getLogger( SerializationUtil.class );

	private SerializationUtil() {
		// Private constructor, do not use it.
	}

	public static String serialize(Object object) throws IOException {
		try ( ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream( baos ) ) {
			oos.writeObject( object );
			oos.flush();
			byte bytes[] = baos.toByteArray();
			return Base64.getEncoder().encodeToString( bytes );
		}
	}

	public static Object deserialize(String serialized) throws IOException, ClassNotFoundException {
		if ( StringHelper.isEmpty( serialized ) ) {
			return null;
		}
		byte bytes[] = Base64.getDecoder().decode( serialized );
		try ( ByteArrayInputStream bais = new ByteArrayInputStream( bytes );
				ObjectInputStream ois = new ObjectInputStream( bais ) ) {
			return ois.readObject();
		}
	}
}
