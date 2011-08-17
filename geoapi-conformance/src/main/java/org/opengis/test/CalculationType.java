/*
 *    GeoAPI - Java interfaces for OGC/ISO standards
 *    http://www.geoapi.org
 *
 *    Copyright (C) 2008-2011 Open Geospatial Consortium, Inc.
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
package org.opengis.test;

import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.test.referencing.TransformTestCase;


/**
 * The kind of calculation that produced the {@link DirectPosition}s being compared.
 * This enumeration is expected by all {@code assertCoordinateEquals(...)} methods in the
 * {@link TransformTestCase} class. The enumeration values are consumed by the following
 * methods:
 * <p>
 * <ul>
 *   <li>{@link TransformTestCase#normalize(DirectPosition, DirectPosition, CalculationType)}</li>
 *   <li>{@link ToleranceModifier#adjust(double[], DirectPosition, CalculationType)}</li>
 * </ul>
 * <p>
 * For example if a precision of 10 centimetres is requested for a map projection, then that
 * tolerance threshold must be converted from metres to decimal degrees (approximatively 1E-6°)
 * when testing the inverse projection. This enumeration allows {@link ToleranceModifier}
 * implementations to know when such conversion is needed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 3.1
 * @since   3.1
 */
public enum CalculationType {
    /**
     * The ordinate values to compare are the result of an identity operation (a plain copy).
     * Ordinate values are checked for strict equality.
     */
    STRICT,

    /**
     * The ordinate values to compare are the result of a direct operation performed by the
     * {@linkplain TransformTestCase#transform transform to test}.
     */
    DIRECT_TRANSFORM,

    /**
     * The ordinate values to compare are the result of the
     * {@linkplain MathTransform#inverse() inverse transform}.
     */
    INVERSE_TRANSFORM
}