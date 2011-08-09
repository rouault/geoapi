/*
 *    GeoAPI - Java interfaces for OGC/ISO standards
 *    http://www.geoapi.org
 *
 *    Copyright (C) 2011 Open Geospatial Consortium, Inc.
 *    All Rights Reserved. http://www.opengeospatial.org/ogc/legal
 *
 *    Permission to use, copy, and modify this software and its documentation, with
 *    or without modification, for any purpose and without fee or royalty is hereby
 *    granted, provided that you include the following on ALL copies of the software
 *    and documentation or portions thereof, including modifications, that you make:
 *
 *    1. The full text of this NOTICE in a location viewable to users of the
 *       redistributed or derivative work.
 *    2. Notice of any changes or modifications to the OGC files, including the
 *       date changes were made.
 *
 *    THIS SOFTWARE AND DOCUMENTATION IS PROVIDED "AS IS," AND COPYRIGHT HOLDERS MAKE
 *    NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 *    TO, WARRANTIES OF MERCHANTABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR THAT
 *    THE USE OF THE SOFTWARE OR DOCUMENTATION WILL NOT INFRINGE ANY THIRD PARTY
 *    PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER RIGHTS.
 *
 *    COPYRIGHT HOLDERS WILL NOT BE LIABLE FOR ANY DIRECT, INDIRECT, SPECIAL OR
 *    CONSEQUENTIAL DAMAGES ARISING OUT OF ANY USE OF THE SOFTWARE OR DOCUMENTATION.
 *
 *    The name and trademarks of copyright holders may NOT be used in advertising or
 *    publicity pertaining to the software without specific, written prior permission.
 *    Title to copyright in this software and any associated documentation will at all
 *    times remain with copyright holders.
 */
package org.opengis.test.referencing;

import java.util.Arrays;
import java.awt.geom.Rectangle2D;
import org.opengis.referencing.operation.CoordinateOperation;

import static org.opengis.test.referencing.PseudoEpsgFactory.FEET;


/**
 * Sample points given in the EPSG guidance document or other authoritative sources. The sample
 * points are used for testing a {@linkplain CoordinateOperation coordinate operation}, which is
 * typically (but not necessarily) a map projection. The coordinate operation being tested is
 * identified by the {@linkplain #operation} field.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 3.1
 * @since   3.1
 */
final class SamplePoints {
    /**
     * The EPSG code for the <em>target</em> Coordinate Reference System of the sample points.
     * This is typically a projected CRS. The <em>source</em> CRS is typically the base CRS of
     * that projected CRS.
     */
    final int targetCRS;

    /**
     * The EPSG code of the {@linkplain CoordinateOperation coordinate operation} from the
     * base CRS to the {@linkplain #targetCRS target CRS}.
     */
    final int operation;

    /**
     * The points to test, in the source (typically geographic) CRS.
     */
    final double[] sourcePoints;

    /**
     * The expected results of the conversion or transformation of {@link #sourcePoints}.
     */
    final double[] targetPoints;

    /**
     * The area of validity in which to test random points, in units of the base (source) CRS.
     */
    final Rectangle2D areaOfValidity;

    /**
     * Creates a new instance for the given sample points.
     */
    private SamplePoints(final int targetCRS, final int operation,
            double[] sourcePoints, double[] targetPoints, final Rectangle2D areaOfValidity)
    {
        this.targetCRS      = targetCRS;
        this.operation      = operation;
        this.sourcePoints   = sourcePoints;
        this.targetPoints   = targetPoints;
        this.areaOfValidity = areaOfValidity;
    }

    /**
     * Creates an object containing sample values for a map projection from the base to the given
     * projected CRS. The CRS codes accepted by this method are the same than the ones documented
     * in the second column of {@link PseudoEpsgFactory#createParameters(int)}.
     */
    static SamplePoints getSamplePoints(final int crs) {
        final double λ0;   // Longitude of natural origin
        final double φ0;   // Latitude of natural origin
        final double fe;   // False easting
        final double fn;   // False northing
        final double λ, φ, e, n;
        final double λmin, λmax, φmin, φmax;
        final int operation;
        switch (crs) {
            case 3002: {  // "Makassar / NEIEZ"
                operation = 19905;
                fe =   3900000.00;  λ0 = 110;
                fn =    900000.00;  φ0 =   0;
                e  =   5009726.58;  λ  = 120;
                n  =    569150.82;  φ  =  -3;
                λmin = 117.6; φmin = -7.9;
                λmax = 121.0; φmax =  2.0;
                break;
            }
            case 3388: {  // "Pulkovo 1942 / Caspian Sea Mercator"
                operation = 19884;
                fe =         0.00;  λ0 = 51;
                fn =         0.00;  φ0 =  0;
                e  =    165704.29;  λ  = 53;
                n  =   5171848.07;  φ  = 53;
                λmin = 46.68; φmin = 36.58;
                λmax = 54.76; φmax = 47.11;
                break;
            }
            case 3857: {  // "WGS 84 / Pseudo-Mercator"
                operation = 3856;
                fe =         0.00;  λ0 = 0;
                fn =         0.00;  φ0 = 0;
                e  = -11169055.58;  λ  = -(100 +       20.0  /60);     // 100°20'00.000"W
                n  =   2800000.00;  φ  =  ( 24 + (22 + 54.433/60)/60); //  24°22'54.433"N
                λmin = -180.0; φmin = -85.0;
                λmax =  180.0; φmax =  85.0;
                break;
            }
            case 24200: {  // "JAD69 / Jamaica National Grid"
                operation = 19910;
                fe =    250000.00;  λ0 = -77.0;
                fn =    150000.00;  φ0 =  18.0;
                e  =    255966.58;  λ  = -(76 + (56 + 37.26/60)/60);   // 76°56'37.26"W
                n  =    142493.51;  φ  =  (17 + (55 + 55.80/60)/60);   // 17°55'55.80"N
                λmin = -78.4; φmin = 17.65;
                λmax = -76.1; φmax = 18.6;
                break;
            }
            case 32040: { // "NAD27 / Texas South Central"
                operation = 14204;
                fe = 2000000.00/FEET;  λ0 = -99.0;
                fn =       0.00/FEET;  φ0 =  27 + 50.0/60;
                e  = 2963503.91/FEET;  λ  = -96.0;          // 96°00'00.00"W
                n  =  254759.80/FEET;  φ  =  28 + 30.0/60;  // 28°30'00.00"N
                λmin = -105.0; φmin = 27.82;
                λmax = -93.41; φmax = 30.66;
                break;
            }
            case 31300: {  // "Belge 1972 / Belge Lambert 72"
                operation = 19902;
                fe =    150000.01;  λ0 =  4 + (21 + 24.983/60)/60;
                fn =   5400088.44;  φ0 = 90;
                e  =    251763.20;  λ  =  5 + (48 + 26.533/60)/60;  //  5°48'26.533"E
                n  =    153034.13;  φ  = 50 + (40 + 46.461/60)/60;  // 50°40'46.461"N
                λmin = 2.54; φmin = 49.51;
                λmax =  6.4; φmax = 51.5;
                break;
            }
            case 3035: {  // "ETRS89 / LAEA Europe"
                operation = 19986;
                fe =   4321000.00;  λ0 = 10;
                fn =   3210000.00;  φ0 = 52;
                e  =   3962799.45;  λ  =  5;
                n  =   2999718.85;  φ  = 50;
                λmin = -31.53; φmin = 27.75;
                λmax =  45.00; φmax = 71.15;
                break;
            }
            case 310642901: {  // "IGNF:MILLER"
                operation = 310642901;
                fe =         0.00;  λ0 =  0;
                fn =         0.00;  φ0 =  0;
                e  =    275951.78;  λ  =  2.478917;
                n  =   5910061.78;  φ  = 48.805639;
                λmin = -180.0; φmin = -90.0;
                λmax =  180.0; φmax =  90.0;
                break;
            }
            default: throw new IllegalArgumentException("No sample points for EPSG:" + crs);
        }
        return new SamplePoints(crs, operation, new double[] {λ0, φ0, λ, φ}, new double[] {fe, fn, e, n},
                new Rectangle2D.Double(λmin, φmin, λmax - λmin, φmax - φmin));
    }

    /**
     * Returns a string representation of the sample points, for debugging purpose.
     */
    @Override
    public String toString() {
        return "SamplePoints[" + Arrays.toString(sourcePoints) + " ⇒ " +
                Arrays.toString(targetPoints) + " in EPSG:" + targetCRS + ']';
    }
}