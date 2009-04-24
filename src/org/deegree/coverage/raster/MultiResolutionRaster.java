//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 ---------------------------------------------------------------------------*/
package org.deegree.coverage.raster;

import static java.lang.Math.abs;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.deegree.coverage.AbstractCoverage;
import org.deegree.coverage.raster.geom.RasterReference;
import org.deegree.geometry.Envelope;

/**
 * This class represents a collection of {@link AbstractRaster} instances that describe the same spatial region, but
 * with different resolutions.
 * 
 * @author <a href="mailto:tonnhofer@lat-lon.de">Oliver Tonnhofer</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class MultiResolutionRaster extends AbstractCoverage {

    private List<AbstractRaster> resolutions = new LinkedList<AbstractRaster>();

    /**
     * Adds a raster to the MultiResolution Pyramid
     * 
     * @param raster
     *            raster to be added to the MultiResolutionRaster
     */
    public void addRaster( AbstractRaster raster ) {
        resolutions.add( raster );
        Comparator<AbstractRaster> comp = new Comparator<AbstractRaster>() {
            public int compare( AbstractRaster a1, AbstractRaster a2 ) {
                double r1 = Math.abs( a1.getRasterReference().getXRes() );
                double r2 = Math.abs( a2.getRasterReference().getXRes() );
                return Double.valueOf( r1 ).compareTo( r2 );
            }
        };
        Collections.sort( resolutions, comp );
    }

    /**
     * Returns the best-fitting raster for a given resolution.
     * 
     * This method tries to return the optimal raster for the requested resolution. It returns the next best resolution
     * (lower resolution value) if available, otherwise it returns the next raster with a higher resolution value.
     * 
     * @param res
     *            resolution in world units per pixel
     * @return raster for resolution or <code>null</code> if no rasters were defined
     */
    public AbstractRaster getRaster( double res ) {
        Iterator<AbstractRaster> iter = resolutions.iterator();
        AbstractRaster prevRaster = null;
        if ( iter.hasNext() ) {
            prevRaster = iter.next();
            boolean found = false;
            while ( !found && iter.hasNext() ) {
                AbstractRaster curRaster = iter.next();
                if ( curRaster.getRasterReference().getXRes() > res ) {
                    found = true;
                } else {
                    prevRaster = curRaster;
                }
            }
        }
        return prevRaster;
    }

    @Override
    public Envelope getEnvelope() {
        // return envelope of highest resolution.
        // envelopes of other resolutions can be larger due to padding of tiles
        return resolutions.get( 0 ).getEnvelope();
    }

    /**
     * Returns a subset of the raster for a given resolution.
     * 
     * The matching raster is selected using {@link #getRaster(double)}.
     * 
     * @see #getRaster(double)
     * 
     * @param envelope
     *            envelope of the subset
     * @param resolution
     *            resolution in world units per pixel
     * @return subset of the best-fitting raster
     */
    public AbstractRaster getSubset( Envelope envelope, double resolution ) {
        AbstractRaster raster = getRaster( resolution );
        return raster.getSubRaster( envelope );
    }

    /**
     * Returns a list with the highest resolution of every level. The list is sorted ascending (from highest to lowest
     * resolution).
     * 
     * @return a list of all resolutions
     */
    public List<Double> getResolutions() {
        List<Double> res = new ArrayList<Double>( resolutions.size() );
        for ( AbstractRaster level : resolutions ) {
            RasterReference renv = level.getRasterReference();
            res.add( min( abs( renv.getXRes() ), abs( renv.getYRes() ) ) );
        }
        return res;
    }
}
