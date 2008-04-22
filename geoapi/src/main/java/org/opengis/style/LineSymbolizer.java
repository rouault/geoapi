/*$************************************************************************************************
 **
 ** $Id$
 **
 ** $URL$
 **
 ** Copyright (C) 2008 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.style;

import org.opengis.annotation.XmlElement;
import org.opengis.filter.expression.Expression;


/**
 * Gives directions for how to draw lines on a map.
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/symbol">Symbology Encoding Implementation Specification 1.1.0</A>
 * @author Open Geospatial Consortium
 * @author Johann Sorel (Geomatys)
 * @author Chris Dillard (SYS Technologies)
 * @since GeoAPI 2.2
 */
@XmlElement("LineSymbolizer")
public interface LineSymbolizer extends Symbolizer {
    
    /**
     * Returns the object containing all the information necessary to draw
     * styled lines.
     * @return Stroke object, contain information about how to draw lines
     */
    @XmlElement("Stroke")
    Stroke getStroke();

    /**
     * Sets the object containing all the information necessary to draw styled lines.
     * See {@link #getStroke} for details.
     * @param stroke : new stroke 
     */
    @XmlElement("Stroke")
    void setStroke(Stroke stroke);
        
    /**
     * PerpendicularOffset allows to draw lines in parallel to the original geometry. For
     * complex line strings these parallel lines have to be constructed so that the distance
     * between original geometry and drawn line stays equal. This construction can result in
     * drawn lines that are actually smaller or longer than the original geometry.
     * 
     * The distance is in uoms and is positive to the left-hand side of the line string. Negative
     * numbers mean right. The default offset is 0.

     * @return Expression
     */
    @XmlElement("PerpendicularOffset")
    Expression getPerpendicularOffset();
    
    /**
     * Set the new perpendicular offset
     * See {@link #getPerpendicularOffSet} for details.
     * 
     * @param exp : new perpendicular offset
     */
    @XmlElement("PerpendicularOffset")
    void setPerpendicularOffset(Expression exp);
}
