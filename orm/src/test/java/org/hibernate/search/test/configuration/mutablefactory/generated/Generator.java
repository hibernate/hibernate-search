/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.test.configuration.mutablefactory.generated;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Emmanuel Bernard
 */
public final class Generator {

	private Generator() {
		//not allowed
	}

	public static void main(String[] args) {
		StringBuilder generated = new StringBuilder( );
		generated.append( "package org.hibernate.search.test.configuration.mutablefactory.generated;\n\n" )
			.append( "import org.hibernate.search.annotations.DocumentId;\n" )
			.append( "import org.hibernate.search.annotations.Field;\n" )
			.append( "import org.hibernate.search.annotations.Indexed;\n\n" )
			.append( "/** Class generated container 100 inner classes */" )
			.append( "public class Generated {\n" );

		StringBuilder inner = new StringBuilder( );
		inner.append( "\t" ).append( "@Indexed").append( "\n" )
				.append( "\t" ).append( "public static class Ax {" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "public Ax(Integer id, String name) { this.id = id; this.name = name; }" ).append( "\n\n" )
				.append( "\t" ).append( "\t" ).append( "@DocumentId" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "public Integer getId() {return id;}" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "public void setId(Integer id) { this.id = id; }" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "private Integer id;" ).append( "\n\n" )
				.append( "\t" ).append( "\t" ).append( "@Field" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "public String getName() {return name;}" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "public void setName(String name) { this.name = name; }" ).append( "\n" )
				.append( "\t" ).append( "\t" ).append( "private String name;" ).append( "\n" )
				.append( "\t}\n\n" );
		String innerString = inner.toString();
		for ( int i = 0 ; i < 100 ; i++ ) {
			generated.append( innerString.replace( "Ax", "A" + i ) );
		}

		generated.append( "}\n" );
		File f = new File( "./Generated.java" );
		try {
			FileWriter fw = new FileWriter( f );
			fw.write( generated.toString() );
			fw.close();
		}
		catch (IOException e) {
			System.out.println( "Error while generating classes" );
			e.printStackTrace();
		}
		System.out.println( "Generated in :" + f.getAbsolutePath() );
	}
}
