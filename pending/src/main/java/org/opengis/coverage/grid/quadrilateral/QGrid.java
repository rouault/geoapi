/*$************************************************************************************************
 **
 ** $Id: Grid.java 658 2006-02-22 18:09:34 -0700 (Wed, 22 Feb 2006) Desruisseaux $
 **
 ** $Source$
 **
 ** Copyright (C) 2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.coverage.grid.quadrilateral;

// J2SE direct dependencies
import java.util.List;
import java.util.Set;

// OpenGIS dependencies
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Annotations
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Contains the geometric characteristics of a qualdrilateral grid. A grid is a network composed
 * of two or more sets of curves in which members of each set intersect the members of other sets
 * in a systematic way. The curves are called <cite>grid lines</cite>; the points at which they
 * intersect are <cite>grid points</cite>; the interstices between the grid lines are called
 * <cite>grid cells</cite>.
 * <p>
 * {@code Grid} has three subclasses, which lie in two partitions. The Positioning partition includes
 * {@link RectifiedGrid} and {@link ReferenceableGrid}, which contain information that relates the grid
 * coordinates to an external {@linkplain CoordinateReferenceSystem coordinate reference system}. The
 * Valuation partition includes {@link GridValuesMatrix}, which contains information for assigning
 * values from the range to each of the grid points.
 * <p>
 * {@code Grid} is not an abstract class: an instance of {@code Grid} need not be an instance of any
 * of its subclasses. The partitions indicate that an instance of the subclass {@link GridValuesMatrix}
 * may be, at the same time, an instance of either the subclass {@link RectifiedGrid} or of the subclass
 * {@link ReferenceableGrid}.
 * 
 * @author Martin Schouwenburg
 * @author Wim Koolhoven
 * @author Martin Desruisseaux
 */
@UML(identifier="CV_Grid", specification=ISO_19123)
public interface Grid {
	/**
	 * Returns the dimensionality of the grid. The dimensionality is the number
	 * of definining curve sets that constitute the grid.
	 */
    @UML(identifier="dimension", obligation=MANDATORY, specification=ISO_19123)
	int getDimension();

	/**
	 * Returns a list containing the names of the grid axes. Each name is
	 * linked to one of the defining curve sets that constitute the grid.
	 */
    @UML(identifier="axisNames", obligation=MANDATORY, specification=ISO_19123)
	List<String> getAxisNames();

	/**
	 * Returns the limits of a section of the grid. The envelope contains the low
     * and high coordinates of the minimal envelope that can contain the grid.
	 */
    @UML(identifier="extent", obligation=OPTIONAL, specification=ISO_19123)
	GridEnvelope getExtent();

	/**
	 * Returns the set of {@linkplain GridPoint grid points} that are located at the
	 * intersections of the grid lines. The collection contains one or more grid points.
     *
     * @see GridPoint#getFramework
	 */
    @UML(identifier="intersection", obligation=MANDATORY, specification=ISO_19123)
	Set<GridPoint> getIntersections();

	/**
	 * Returns the set of {@linkplain GridCell grid cells} delineated by the grid lines.
     * The collection contains one or more grid cells.
     *
     * @see GridCell#getFramework
	 */
    @UML(identifier="cell", obligation=MANDATORY, specification=ISO_19123)
	Set<GridCell> getCells();
	
	/**
	 * Specified in ISO 19123 as a “partition” of an inheritance relation, 
	 * the valuation facility is recast here as a composition association.  
	 * This increases clarity and eliminates the required multiple inheritance.  
	 * The valuation association organizes the multi-dimensional grid 
	 * into a linear sequence of values according to a limited number of specifiable schemes.
	 */
	@Extension
	GridValuesMatrix getValuation();
	
	/**
	 * Specified in ISO 19123 as a “partition” of an inheritance relation, 
	 * the positioning facility is recast here as a composition association.  
	 * This increases clarity and eliminates the required multiple inheritance.  
	 * The positioning association shall link this grid with an object capable of 
	 * transforming the grid coordinates into a representation in an external coordinate reference system.  
	 * The associated object may be either a RectifiedGrid or a ReferenceableGrid, 
	 * but shall not be only a GridPositioning object.
	 */
	@Extension
	GridPositioning getAxisNames();
}