/*
 *    GeoAPI - Java interfaces for OGC/ISO standards
 *    http://www.geoapi.org
 *
 *    This file is hereby placed into the Public Domain.
 *    This means anyone is free to do whatever they wish with this file.
 *
 *    The NetCDF wrappers are provided as code examples, in the hope to facilitate
 *    GeoAPI implementations backed by other libraries. Implementors can take this
 *    source code and use it for any purpose, commercial or non-commercial, copyrighted
 *    or open-source, with no legal obligation to acknowledge the borrowing/copying
 *    in any way.
 */
package org.opengis.wrapper.netcdf;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.unit.SI;

import ucar.nc2.units.DateUnit;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateAxis1DTime;

import org.opengis.metadata.extent.Extent;
import org.opengis.util.InternationalString;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.coverage.grid.GridEnvelope;

import org.opengis.example.referencing.SimpleCRS;
import org.opengis.example.referencing.SimpleDatum;
import org.opengis.example.referencing.SimpleMatrix;
import org.opengis.example.coverage.SimpleGridEnvelope;


/**
 * Wraps a NetCDF {@link CoordinateSystem} as an implementation of GeoAPI interfaces.
 * This class implements both the GeoAPI {@link org.opengis.referencing.cs.CoordinateSystem} and
 * {@link CoordinateReferenceSystem} interfaces because the NetCDF {@code CoordinateSystem}
 * object combines the concepts of both of them. It also implements the {@link GridGeometry}
 * interface since NetCDF Coordinate Systems contain all information related to the image grid.
 * <p>
 * <b>Axis order</b><br>
 * The order of axes returned by {@link #getAxis(int)} is reversed compared to the order of axes
 * in the wrapped NetCDF coordinate system. This is because the NetCDF convention stores axes in
 * the (<var>time</var>, <var>height</var>, <var>latitude</var>, <var>longitude</var>) order, while
 * referencing framework often uses the (<var>longitude</var>, <var>latitude</var>, <var>height</var>,
 * <var>time</var>) order.
 * <p>
 * <b>Restrictions</b><br>
 * Current implementation has the following restrictions:
 * <ul>
 *   <li><p>This class supports only axes of kind {@link CoordinateAxis1D}. Callers can verify this
 *       condition with a call to the {@link CoordinateSystem#isProductSet()} method on the wrapped
 *       NetCDF coordinate system, which shall returns {@code true}.</p></li>
 *
 *   <li><p>At the time of writing, the NetCDF API doesn't specify the CRS datum. Consequently the
 *       current implementation assumes that all {@code NetcdfCRS} instances use the
 *       {@linkplain org.opengis.example.referencing.SimpleDatum#WGS84 WGS84}
 *       geodetic datum.</p></li>
 *
 *   <li><p>This class assumes that the list of NetCDF axes returned by
 *       {@link CoordinateSystem#getCoordinateAxes()} is stable during the
 *       lifetime of this {@code NetcdfCRS} instance.</p></li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 3.1
 * @since   3.1
 */
public class NetcdfCRS extends NetcdfIdentifiedObject implements CoordinateReferenceSystem,
        org.opengis.referencing.cs.CoordinateSystem, GridGeometry
{
    /**
     * Small tolerance factor for rounding error.
     */
    private static final double EPS = 1E-10;

    /**
     * The NetCDF coordinate system wrapped by this {@code NetcdfCRS} instance.
     */
    private final CoordinateSystem cs;

    /**
     * The NetCDF axes.
     */
    private final NetcdfAxis[] axes;

    /**
     * The grid envelope extent, computed when first needed.
     */
    private transient GridEnvelope extent;

    /**
     * The grid to CRS transform, computed when first needed.
     */
    private transient MathTransform gridToCRS;

    /**
     * Creates a new {@code NetcdfCRS} object wrapping the same NetCDF coordinate system
     * than the given object. This copy constructor is provided for subclasses wanting to
     * wraps the same NetCDF coordinate system and change a few properties or methods.
     *
     * @param crs The CRS to copy.
     */
    protected NetcdfCRS(final NetcdfCRS crs) {
        this.cs        = crs.cs;
        this.axes      = crs.axes;
        this.extent    = crs.extent;
        this.gridToCRS = crs.gridToCRS;
    }

    /**
     * Creates a new {@code NetcdfCRS} object wrapping the given NetCDF coordinate system.
     * The {@link CoordinateSystem#getCoordinateAxes()} is invoked at construction time and
     * every elements are assumed instances of {@link CoordinateAxis1D}.
     *
     * @param  netcdfCS The NetCDF coordinate system to wrap.
     * @throws ClassCastException If at least one axis is not an instance of the
     *         {@link CoordinateAxis1D} subclass.
     */
    protected NetcdfCRS(final CoordinateSystem netcdfCS) throws ClassCastException {
        this(netcdfCS, netcdfCS.getCoordinateAxes());
    }

    /**
     * Creates a new {@code NetcdfCRS} object wrapping the given axes of the given NetCDF
     * coordinate system. The axes will be retained in reverse order, as documented in
     * class javadoc.
     *
     * @param  netcdfCS The NetCDF coordinate system to wrap.
     * @param  The axes to add, in reverse order.
     */
    NetcdfCRS(final CoordinateSystem netcdfCS, final List<CoordinateAxis> netcdfAxis) {
        Objects.requireNonNull(netcdfCS);
        cs = netcdfCS;
        final int dimension = netcdfAxis.size();
        axes = new NetcdfAxis[dimension];
        for (int i=0; i<dimension; i++) {
            // Adds the axis in reverse order. See class javadoc for explanation.
            axes[(dimension-1) - i] = new NetcdfAxis((CoordinateAxis1D) netcdfAxis.get(i));
        }
    }

    /**
     * Creates a new {@code NetcdfCRS} with {@link NetcdfAxis} instances fetched
     * from the given components. This is used by the {@link Compound} constructor.
     */
    NetcdfCRS(final CoordinateSystem netcdfCS, final NetcdfCRS... components) {
        cs = netcdfCS;
        final List<NetcdfAxis> axes = new ArrayList<NetcdfAxis>(netcdfCS.getRankRange());
        for (final NetcdfCRS c : components) {
            axes.addAll(Arrays.asList(c.axes));
        }
        this.axes = axes.toArray(new NetcdfAxis[axes.size()]);
    }

    /**
     * Creates a new {@code NetcdfCRS} object wrapping the given NetCDF coordinate system.
     * The returned object may implement any of the {@link ProjectedCRS}, {@link GeographicCRS}
     * {@link VerticalCRS} or {@link TemporalCRS}, depending on the {@linkplain AxisType axis
     * types}.
     * <p>
     * If the NetCDF object contains different kind of CRS, then the returned CRS will be an
     * instance of {@link CompoundCRS} in which each component implements one of the above-cited
     * interfaces.
     * <p>
     * If the NetCDF object contains axes of unknown type, then the returned CRS will not
     * implement any of the above-cited interfaces.
     *
     * @param  netcdfCS The NetCDF coordinate system to wrap, or {@code null} if none.
     * @return A wrapper for the given object, or {@code null} if the argument was null.
     * @throws ClassCastException If at least one axis is not an instance of the
     *         {@link CoordinateAxis1D} subclass.
     */
    public static NetcdfCRS wrap(final CoordinateSystem netcdfCS) throws ClassCastException {
        try {
            return wrap(netcdfCS, null, null);
        } catch (IOException e) {
            throw new AssertionError(e); // Should never happen, since we didn't performed any I/O.
        }
    }

    /**
     * Creates a new {@code NetcdfCRS} object, optionally using the given NetCDF file for additional
     * information. This method performs the same work than {@link #wrap(CoordinateSystem)}, except
     * that more accurate coordinate axes may be created if a reference to the original dataset file
     * is provided. This apply especially to {@link CoordinateAxis1DTime}.
     *
     * @param  netcdfCS The NetCDF coordinate system to wrap, or {@code null} if none.
     * @param  file The originating dataset file, or {@code null} if none.
     * @param  logger An optional object where to log warnings, or {@code null} if none.
     * @return A wrapper for the given object, or {@code null} if the {@code netcdfCS}
     *         argument was null.
     * @throws ClassCastException If at least one axis is not an instance of the
     *         {@link CoordinateAxis1D} subclass.
     * @throws IOException If an I/O operation was needed and failed.
     *
     * @since 3.14
     */
    public static NetcdfCRS wrap(final CoordinateSystem netcdfCS, final NetcdfDataset file,
                final Logger logger) throws IOException, ClassCastException
    {
        if (netcdfCS == null) {
            return null;
        }
        /*
         * Separate the horizontal, vertical and temporal components. We need to iterate
         * over the Netcdf axes in reverse order (see class javadoc). We don't use the
         * CoordinateAxis.getTaxis() and similar methods because we want to ensure that
         * the components are build in the same order than axes are found.
         */
        final List<NetcdfCRS> components = new ArrayList<NetcdfCRS>(4);
        final List<CoordinateAxis>  axes = netcdfCS.getCoordinateAxes();
        for (int i=axes.size(); --i>=0;) {
            CoordinateAxis1D axis = (CoordinateAxis1D) axes.get(i);
            if (axis != null) {
                final AxisType type = axis.getAxisType();
                if (type != null) { // This is really null in some NetCDF file.
                    switch (type) {
                        case Pressure:
                        case Height:
                        case GeoZ: {
                            components.add(new Vertical(netcdfCS, axis));
                            continue;
                        }
                        case RunTime:
                        case Time: {
                            components.add(new Temporal(netcdfCS, Temporal.complete(axis, file, logger)));
                            continue;
                        }
                        case Lat:
                        case Lon: {
                            final int upper = i+1;
                            i = lower(axes, i, AxisType.Lat, AxisType.Lon);
                            components.add(new Geographic(netcdfCS, axes.subList(i, upper)));
                            continue;
                        }
                        case GeoX:
                        case GeoY: {
                            final int upper = i+1;
                            i = lower(axes, i, AxisType.GeoX, AxisType.GeoY);
                            components.add(new Projected(netcdfCS, axes.subList(i, upper)));
                            continue;
                        }
                    }
                }
            }
            // Unknown axes: do not try to split.
            components.clear();
            break;
        }
        final int size = components.size();
        switch (size) {
            /*
             * If we have been unable to split the CRS ourself in various components,
             * use the information provided by the NetCDF library as a fallback. Note
             * that the CRS created that way may not be valid in the ISO 19111 sense.
             */
            case 0: {
                if (netcdfCS.isLatLon()) {
                    return new Geographic(netcdfCS, axes);
                }
                if (netcdfCS.isGeoXY()) {
                    return new Projected(netcdfCS, axes);
                }
                return new NetcdfCRS(netcdfCS, axes);
            }
            /*
             * If we have been able to create exactly one CRS, returns that CRS.
             */
            case 1: {
                return components.get(0);
            }
            /*
             * Otherwise create a CompoundCRS will all the components we have separated.
             */
            default: {
                return new Compound(netcdfCS, components.toArray(new NetcdfCRS[size]));
            }
        }
    }

    /**
     * Returns the lower index of the sublist containing axes of the given types.
     *
     * @param axes  The list from which to get the sublist indices.
     * @param upper The upper index of the sublist, inclusive.
     * @param t1    The first axis type to accept.
     * @param t2    The second axis type to accept.
     * @return      The lower index of the sublist range.
     */
    private static int lower(final List<CoordinateAxis> axes, int upper, final AxisType t1, final AxisType t2) {
        while (upper != 0) {
            final AxisType type = axes.get(upper-1).getAxisType();
            if (type != t1 && type != t2) {
                break;
            }
            upper--;
        }
        return upper;
    }

    /**
     * Returns the wrapped NetCDF coordinate system.
     * <p>
     * <b>Note:</b> The dimension of the returned NetCDF Coordinate System may be greater than the
     * dimension of the GeoAPI CRS implemented by this object, because the NetCDF CS puts all axes
     * in a single object while the GeoAPI CRS may splits the axes in various kind of CRS
     * ({@link GeographicCRS}, {@link VerticalCRS}, {@link TemporalCRS}).
     */
    @Override
    public CoordinateSystem delegate() {
        return cs;
    }

    /**
     * Returns the coordinate system name. The default implementation delegates to
     * {@link CoordinateSystem#getName()}.
     *
     * @see CoordinateSystem#getName()
     */
    @Override
    public String getCode() {
        return cs.getName();
    }

    /**
     * Returns the number of dimensions.
     *
     * @see CoordinateSystem#getRankRange()
     */
    @Override
    public int getDimension() {
        return axes.length;
    }

    /**
     * Returns the coordinate system, which is {@code this}.
     */
    @Override
    public org.opengis.referencing.cs.CoordinateSystem getCoordinateSystem() {
        return this;
    }

    /**
     * Returns the axis at the given dimension. Note that the order of axes returned by this
     * method is reversed compared to the order of axes in the NetCDF coordinate system. See
     * the <a href="#skip-navbar_top">class javadoc</a> for more information.
     *
     * @param  dimension The zero based index of axis.
     * @return The axis at the specified dimension.
     * @throws IndexOutOfBoundsException if {@code dimension} is out of bounds.
     *
     * @see CoordinateSystem#getCoordinateAxes()
     */
    @Override
    public NetcdfAxis getAxis(final int dimension) throws IndexOutOfBoundsException {
        return axes[dimension];
    }

    /**
     * Returns the valid coordinate range of the NetCDF grid coordinates.
     * The lowest valid grid coordinate is zero.
     *
     * @return The valid coordinate range of a grid coverage.
     *
     * @since 3.20 (derived from 3.09)
     */
    @Override
    public synchronized GridEnvelope getExtent() {
        if (extent == null) {
            final int[] lower = new int[axes.length];
            final int[] upper = new int[axes.length];
            for (int i=0; i<upper.length; i++) {
                upper[i] = axes[i].length() - 1;
            }
            extent = new SimpleGridEnvelope(lower, upper);
        }
        return extent;
    }

    /**
     * @deprecated Renamed {@link #getExtent()}.
     */
    @Override
    @Deprecated
    public final GridEnvelope getGridRange() {
        return getExtent();
    }

    /**
     * Returns the transform from grid coordinates to this CRS coordinates, or {@code null} if
     * none. If this CRS is regular and two-dimensional, then the returned transform is also an
     * instance of Java2D {@link java.awt.geom.AffineTransform}.</li>
    * <p>
    * <b>Limitation</b><br>
     * Current implementation can build a transform only for regular coordinate systems.
     * A future implementation may be more general.
     *
     * @return The transform from grid to this CRS, or {@code null} if none.
     */
    @Override
    public synchronized MathTransform getGridToCRS() {
        if (gridToCRS == null) {
            gridToCRS = getGridToCRS(0, axes.length);
        }
        return gridToCRS;
    }

    /**
     * Returns the transform from grid coordinates to this CRS coordinates in the given
     * range of dimensions.
    * <p>
    * <b>Limitation</b><br>
     * Current implementation can build a transform only for regular axes.
     * A future implementation may be more general.
     *
     * @param  lowerDimension Index of the first dimension for which to get the transform.
     * @param  upperDimension Index after the last dimension for which to get the transform.
     * @return The transform from grid to this CRS in the given range of dimensions, or
     *         {@code null} if none.
     * @throws IllegalArgumentException If the given dimensions are not in the
     *         [0 &hellip; {@linkplain #getDimension() dimension}] range.
     */
    public MathTransform getGridToCRS(final int lowerDimension, final int upperDimension) {
        if (lowerDimension < 0 || upperDimension > axes.length || upperDimension < lowerDimension) {
            throw new IllegalArgumentException("Illegal range");
        }
        final int numDimensions = upperDimension - lowerDimension;
        final SimpleMatrix matrix = new SimpleMatrix(numDimensions + 1, numDimensions + 1);
        for (int i=0; i<numDimensions; i++) {
            final CoordinateAxis1D axis = axes[lowerDimension + i].delegate();
            if (!axis.isRegular()) {
                return null;
            }
            final double scale = axis.getIncrement();
            if (Double.isNaN(scale) || scale == 0) {
                return null;
            }
            matrix.setElement(i, i, nice(scale));
            matrix.setElement(i, numDimensions, nice(axis.getStart()));
        }
        try {
            return Factories.getFactory(MathTransformFactory.class).createAffineTransform(matrix);
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Workaround rounding errors found in NetCDF files.
     *
     * @since 3.16
     */
    private static double nice(double value) {
        final double tf = value * 360;
        final double ti = Math.rint(tf);
        if (Math.abs(tf - ti) <= EPS) {
            value = ti / 360;
        }
        return value;
    }

    /**
     * Returns {@code null} since NetCDF coordinate systems don't specify their domain
     * of validity.
     */
    @Override
    public Extent getDomainOfValidity() {
        return null;
    }

    /**
     * Returns {@code null} since NetCDF coordinate systems don't specify their scope.
     */
    @Override
    public InternationalString getScope() {
        return null;
    }




    /**
     * The CRS for compound CRS.
     *
     * @author Martin Desruisseaux (Geomatys)
     * @version 3.1
     *
     * @since 3.1
     */
    private static final class Compound extends NetcdfCRS implements CompoundCRS,
            org.opengis.referencing.cs.CoordinateSystem
    {
        /**
         * The components of this compound CRS.
         */
        private final List<CoordinateReferenceSystem> components;

        /**
         * Wraps the given coordinate system.
         */
        Compound(final CoordinateSystem cs, final NetcdfCRS[] components) {
            super(cs, components);
            this.components = Collections.unmodifiableList(Arrays.<CoordinateReferenceSystem>asList(components));
        }

        /**
         * Wraps the same coordinate system than the given CRS, with different components.
         */
        private Compound(final Compound crs, final CoordinateReferenceSystem[] components) {
            super(crs);
            this.components = Collections.unmodifiableList(Arrays.asList(components));
        }

        /**
         * Returns the coordinate system, which is {@code this}.
         */
        @Override
        public org.opengis.referencing.cs.CoordinateSystem getCoordinateSystem() {
            return this;
        }

        /**
         * Returns the components of this compound CRS.
         */
        @Override
        public List<CoordinateReferenceSystem> getComponents() {
            return components;
        }
    }




    /**
     * The CRS, CS and datum for temporal coordinates.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 3.1
     * @since   3.1
     */
    private static final class Temporal extends NetcdfCRS implements TemporalCRS, TimeCS, TemporalDatum {
        /**
         * The date and time origin of this temporal datum.
         */
        private final long origin;

        /**
         * Wraps the given coordinate system.
         */
        Temporal(final CoordinateSystem cs, final CoordinateAxis netcdfAxis) {
            super(cs, Collections.singletonList(netcdfAxis));
            final String unitSymbol = netcdfAxis.getUnitsString();
            final DateUnit unit;
            try {
                unit = new DateUnit(unitSymbol);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unknown unit symbol: " + unitSymbol, e);
            }
            origin = unit.getDateOrigin().getTime();
            getAxis(0).unit = SI.SECOND.times(unit.getTimeUnit().getValueInSeconds());
        }

        /**
         * If the given axis is not an instance of {@link CoordinateAxis1DTime}, tries to build
         * a {@code CoordinateAxis1DTime} now. Otherwise returns the axis unchanged. This method
         * can be invoked before to pass the axis to the constructor, if desired.
         *
         * @param  axis The axis to check.
         * @param  file The originating dataset, or {@code null} if none.
         * @param  logger An optional object where to log warnings, or {@code null} if none.
         * @return The axis as an (@link CoordinateAxis1DTime} if possible.
         * @throws IOException If an I/O operation was needed and failed.
         */
        static CoordinateAxis complete(CoordinateAxis axis, final NetcdfDataset file,
                final Logger logger) throws IOException
        {
            if (!(axis instanceof CoordinateAxis1DTime) && file != null) {
                final Formatter formatter = (logger != null) ? new Formatter() : null;
                axis = CoordinateAxis1DTime.factory(file, axis, formatter);
                if (formatter != null) {
                    final StringBuilder buffer = (StringBuilder) formatter.out();
                    if (buffer.length() != 0) {
                        logger.logp(Level.WARNING, NetcdfCRS.class.getName(), "wrap", buffer.toString());
                    }
                }
            }
            return axis;
        }

        /**
         * Returns the coordinate system, which is {@code this}.
         */
        @Override
        public TimeCS getCoordinateSystem() {
            return this;
        }

        /**
         * Returns the datum.
         */
        @Override
        public TemporalDatum getDatum() {
            return this;
        }

        /**
         * Returns the date and time origin of this temporal datum.
         * The units can be obtained by {@code getAxis(0).getUnit()}.
         */
        @Override
        public Date getOrigin() {
            return new Date(origin);
        }

        /**
         * Returns {@code null} since this simple implementation does not define anchor point.
         */
        @Override
        public InternationalString getAnchorPoint() {
            return null;
        }

        /**
         * Returns {@code null} since this simple implementation does not define realization epoch.
         */
        @Override
        public Date getRealizationEpoch() {
            return null;
        }
    }




    /**
     * The CRS, CS and datum for vertical coordinates.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 3.1
     * @since   3.1
     */
    private static final class Vertical extends NetcdfCRS implements VerticalCRS, VerticalCS, VerticalDatum {
        /**
         * The type of this vertical datum.
         */
        private final VerticalDatumType type;

        /**
         * Wraps the given coordinate system.
         */
        Vertical(final CoordinateSystem cs, final CoordinateAxis netcdfAxis) {
            super(cs, Collections.singletonList(netcdfAxis));
            switch (netcdfAxis.getAxisType()) {
                case Pressure: type = VerticalDatumType.BAROMETRIC;    break;
                case Height:   type = VerticalDatumType.GEOIDAL;       break;
                case GeoZ:     type = VerticalDatumType.valueOf("ELLIPSOIDAL"); break;
                default:       type = VerticalDatumType.OTHER_SURFACE; break;
            }
        }

        /**
         * Returns the coordinate system, which is {@code this}.
         */
        @Override
        public VerticalCS getCoordinateSystem() {
            return this;
        }

        /**
         * Returns the datum.
         */
        @Override
        public VerticalDatum getDatum() {
            return this;
        }

        /**
         * Returns the type of this vertical datum.
         */
        @Override
        public VerticalDatumType getVerticalDatumType() {
            return type;
        }

        /**
         * Returns {@code null} since this simple implementation does not define anchor point.
         */
        @Override
        public InternationalString getAnchorPoint() {
            return null;
        }

        /**
         * Returns {@code null} since this simple implementation does not define realization epoch.
         */
        @Override
        public Date getRealizationEpoch() {
            return null;
        }
    }




    /**
     * The CRS for geographic coordinates. This is normally a two-dimensional CRS (current
     * {@link NetcdfCRS} implementation has no support for 3D geographic CRS). However a
     * different dimension (either 1 or more than 2) may happen for unusual NetCDF files.
     * <p>
     * This class assumes that the geodetic datum is {@linkplain DefaultGeodeticDatum#WGS84 WGS84}.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 3.1
     * @since   3.1
     */
    private static final class Geographic extends NetcdfCRS implements GeographicCRS, EllipsoidalCS {
        /**
         * Wraps the given coordinate system. The given list of axes should in theory contains
         * exactly 2 elements (current {@link NetcdfCRS} implementation has no support for 3D
         * geographic CRS). However a different number of axes may be provided if the
         * {@link NetcdfCRS#wrap(CoordinateSystem)} method has been unable to split the
         * NetCDF coordinate system into geodetic, vertical and temporal components.
         */
        Geographic(final CoordinateSystem cs, final List<CoordinateAxis> netcdfAxis) {
            super(cs, netcdfAxis);
        }

        /**
         * Returns the coordinate system, which is {@code this}.
         */
        @Override
        public EllipsoidalCS getCoordinateSystem() {
            return this;
        }

        /**
         * Returns the datum, which is assumed WGS84.
         */
        @Override
        public GeodeticDatum getDatum() {
            return SimpleDatum.WGS84;
        }
    }




    /**
     * The CRS for projected coordinates. This is normally a two-dimensional CRS. However
     * a different dimension (either 1 or more than 2) may happen for unusual NetCDF files.
     * <p>
     * This class assumes that the geodetic datum is {@linkplain DefaultGeodeticDatum#WGS84 WGS84}.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 3.1
     * @since   3.1
     */
    private static final class Projected extends NetcdfCRS implements ProjectedCRS, CartesianCS {
        /**
         * Wraps the given coordinate system. The given list of axes should in theory contains
         * exactly 2 elements. However a different number of axes may be provided if the
         * {@link NetcdfCRS#wrap(CoordinateSystem)} method has been unable to split the NetCDF
         * coordinate system into geodetic, vertical and temporal components.
         */
        Projected(final CoordinateSystem cs, final List<CoordinateAxis> netcdfAxis) {
            super(cs, netcdfAxis);
        }

        /**
         * Returns the coordinate system, which is {@code this}.
         */
        @Override
        public CartesianCS getCoordinateSystem() {
            return this;
        }

        /**
         * Returns the datum, which is assumed WGS84.
         */
        @Override
        public GeodeticDatum getDatum() {
            return SimpleDatum.WGS84;
        }

        /**
         * Returns the base CRS, which is assumed WGS84.
         */
        @Override
        public GeographicCRS getBaseCRS() {
            return SimpleCRS.Geographic.WGS84;
        }

        /**
         * @todo Not yet implemented.
         */
        @Override
        public Projection getConversionFromBase() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}