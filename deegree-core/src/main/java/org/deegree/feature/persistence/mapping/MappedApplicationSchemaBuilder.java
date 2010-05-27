//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.feature.persistence.mapping;

import static org.deegree.feature.types.property.GeometryPropertyType.CoordinateDimension.DIM_2;
import static org.deegree.feature.types.property.GeometryPropertyType.GeometryType.GEOMETRY;
import static org.deegree.feature.types.property.ValueRepresentation.BOTH;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.deegree.commons.jdbc.ConnectionManager;
import org.deegree.commons.tom.primitive.PrimitiveType;
import org.deegree.commons.utils.Pair;
import org.deegree.cs.CRS;
import org.deegree.feature.persistence.mapping.jaxb.AbstractPropertyDecl;
import org.deegree.feature.persistence.mapping.jaxb.FeatureTypeDecl;
import org.deegree.feature.persistence.mapping.jaxb.GeometryPropertyDecl;
import org.deegree.feature.persistence.mapping.jaxb.Mapping;
import org.deegree.feature.persistence.mapping.jaxb.SimplePropertyDecl;
import org.deegree.feature.types.ApplicationSchema;
import org.deegree.feature.types.FeatureType;
import org.deegree.feature.types.GenericFeatureType;
import org.deegree.feature.types.property.GeometryPropertyType;
import org.deegree.feature.types.property.PropertyType;
import org.deegree.feature.types.property.SimplePropertyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps an {@link ApplicationSchema} and corresponding {@link FeatureTypeMapping}s.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class MappedApplicationSchemaBuilder {

    private static final Logger LOG = LoggerFactory.getLogger( MappedApplicationSchema.class );

    private Map<QName, FeatureType> ftNameToFt = new HashMap<QName, FeatureType>();

    private Map<QName, FeatureTypeMapping> ftNameToMapping = new HashMap<QName, FeatureTypeMapping>();

    private ApplicationSchema schema;

    private DatabaseMetaData md;

    public static MappedApplicationSchema build( Mapping mapping, String jdbcConnId, String dbSchema, CRS storageSRS )
                            throws SQLException {
        MappedApplicationSchemaBuilder builder = new MappedApplicationSchemaBuilder( mapping, jdbcConnId, dbSchema );
        FeatureType[] fts = builder.ftNameToFt.values().toArray( new FeatureType[builder.ftNameToFt.size()] );
        FeatureTypeMapping[] ftMappings = builder.ftNameToMapping.values().toArray(
                                                                                    new FeatureTypeMapping[builder.ftNameToMapping.size()] );
        return new MappedApplicationSchema( fts, null, ftMappings, storageSRS );
    }

    private MappedApplicationSchemaBuilder( Mapping mapping, String connId, String dbSchema ) throws SQLException {

        Connection conn = ConnectionManager.getConnection( connId );
        md = conn.getMetaData();

        for ( FeatureTypeDecl ftDecl : mapping.getFeatureType() ) {
            process( ftDecl );
        }
        schema = new ApplicationSchema( ftNameToFt.values().toArray( new FeatureType[ftNameToFt.size()] ), null );
    }

    private void process( FeatureTypeDecl ftDecl ) {

        QName ftName = ftDecl.getName();
        LOG.info( "Processing feature type '" + ftName + "'" );
        boolean isAbstract = ftDecl.isAbstract() == null ? false : ftDecl.isAbstract();

        String mapping = ftDecl.getMapping();
        if ( mapping == null ) {
            mapping = ftName.getLocalPart().toUpperCase();
            LOG.debug( "No explicit mapping for feature type " + ftName
                       + " specified, defaulting to local feature type name '" + mapping + "'." );
        }

        String fidMapping = ftDecl.getFidMapping();

        List<PropertyType> pts = new ArrayList<PropertyType>();
        Map<QName, String> propToColumn = new HashMap<QName, String>();
        for ( JAXBElement<? extends AbstractPropertyDecl> propDeclEl : ftDecl.getAbstractProperty() ) {
            AbstractPropertyDecl propDecl = propDeclEl.getValue();
            Pair<PropertyType, String> pt = process( propDecl );
            pts.add( pt.first );
            propToColumn.put( pt.first.getName(), pt.second );
        }

        FeatureType ft = new GenericFeatureType( ftName, pts, isAbstract );
        ftNameToFt.put( ftName, ft );

        FeatureTypeMapping ftMapping = new FeatureTypeMapping( ftName, mapping, fidMapping, propToColumn );
        ftNameToMapping.put( ftName, ftMapping );
    }

    private Pair<PropertyType, String> process( AbstractPropertyDecl propDecl ) {

        QName ptName = propDecl.getName();

        int minOccurs = propDecl.getMinOccurs() == null ? 1 : propDecl.getMinOccurs().intValue();
        int maxOccurs = 1;

        if ( propDecl.getMaxOccurs() != null ) {
            if ( propDecl.getMaxOccurs().equals( "unbounded" ) ) {
                maxOccurs = -1;
            } else {
                maxOccurs = Integer.parseInt( propDecl.getMaxOccurs() );
            }
        }

        PropertyType pt = null;
        if ( propDecl instanceof SimplePropertyDecl ) {
            SimplePropertyDecl spt = (SimplePropertyDecl) propDecl;
            PrimitiveType primType = getPrimitiveType( spt.getType() );
            pt = new SimplePropertyType( ptName, minOccurs, maxOccurs, primType, false, null );
        } else if ( propDecl instanceof GeometryPropertyDecl ) {
            GeometryPropertyDecl gpt = (GeometryPropertyDecl) propDecl;
            pt = new GeometryPropertyType( ptName, minOccurs, maxOccurs, GEOMETRY, DIM_2, false, null, BOTH );
        } else {
            throw new RuntimeException( "Internal error: Unhandled property JAXB property type: " + propDecl.getClass() );
        }

        String mapping = propDecl.getMapping();
        if ( mapping == null ) {
            mapping = ptName.getLocalPart().toUpperCase();
            LOG.debug( "No explicit mapping for property " + pt.getName()
                       + " specified, defaulting to local property name '" + mapping + "'." );
        }

        return new Pair<PropertyType, String>( pt, mapping );
    }

    private PrimitiveType getPrimitiveType( org.deegree.feature.persistence.mapping.jaxb.PrimitiveType type ) {
        switch ( type ) {
        case BOOLEAN:
            return PrimitiveType.BOOLEAN;
        case DATE:
            return PrimitiveType.DATE;
        case DATE_TIME:
            return PrimitiveType.DATE_TIME;
        case DECIMAL:
            return PrimitiveType.DECIMAL;
        case DOUBLE:
            return PrimitiveType.DOUBLE;
        case INTEGER:
            return PrimitiveType.INTEGER;
        case STRING:
            return PrimitiveType.STRING;
        case TIME:
            return PrimitiveType.TIME;
        }
        throw new RuntimeException( "Internal error: Unhandled JAXB primitive type: " + type );
    }
}
