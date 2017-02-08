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
import java.util.Collections;
import java.util.Set;

import org.hibernate.criterion.Criterion;
import org.hibernate.search.util.StringHelper;
import org.jboss.logging.Logger;

/**
 * @author Mincong Huang
 */
public class MassIndexerUtil {

	public static final Logger LOGGER = Logger.getLogger( MassIndexerUtil.class );

	public static String serializeCriteria(Set<Criterion> criteria)
			throws IOException {
		try ( ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream( baos ) ) {
			oos.writeObject( criteria );
			oos.flush();
			byte bytes[] = baos.toByteArray();
			return Base64.getEncoder().encodeToString( bytes );
		}
	}

	public static Set<Criterion> deserializeCriteria(String serialized)
			throws IOException, ClassNotFoundException {
		if ( StringHelper.isEmpty( serialized ) ) {
			return Collections.emptySet();
		}
		byte bytes[] = Base64.getDecoder().decode( serialized );
		try ( ByteArrayInputStream bais = new ByteArrayInputStream( bytes );
				ObjectInputStream ois = new ObjectInputStream( bais ) ) {
			@SuppressWarnings("unchecked")
			Set<Criterion> criteria = (Set<Criterion>) ois.readObject();
			return criteria;
		}
	}
}
