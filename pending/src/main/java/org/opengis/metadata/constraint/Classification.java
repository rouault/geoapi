/*$************************************************************************************************
 **
 ** $Id$
 **
 ** $Source$
 **
 ** Copyright (C) 2004-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.metadata.constraint;

// J2SE directdependencies
import java.util.List;
import java.util.ArrayList;

// OpenGIS direct dependencies
import org.opengis.util.CodeList;

// Annotations
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Name of the handling restrictions on the dataset.
 *
 * @version <A HREF="http://www.opengis.org/docs/01-111.pdf">Abstract specification 5.0</A>
 * @author Martin Desruisseaux (IRD)
 * @since GeoAPI 2.0
 */
@UML(identifier="MD_ClassificationCode", specification=ISO_19115)
public final class Classification extends CodeList<Classification> {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -549174931332214797L;

    /**
     * List of all enumerations of this type.
     * Must be declared before any enum declaration.
     */
    private static final List<Classification> VALUES = new ArrayList<Classification>(5);

    /**
     * Available for general disclosure.
     */
    @UML(identifier="unclassified", obligation=CONDITIONAL, specification=ISO_19115)
    public static final Classification UNCLASSIFIED = new Classification("UNCLASSIFIED");

    /**
     * Not for general disclosure.
     */
    @UML(identifier="restricted", obligation=CONDITIONAL, specification=ISO_19115)
    public static final Classification RESTRICTED = new Classification("RESTRICTED");

    /**
     * Available for someone who can be entrusted with information.
     */
    @UML(identifier="confidential", obligation=CONDITIONAL, specification=ISO_19115)
    public static final Classification CONFIDENTIAL = new Classification("CONFIDENTIAL");

    /**
     * Kept or meant to be kept private, unknown, or hidden from all but a select group of people.
     */
    @UML(identifier="secret", obligation=CONDITIONAL, specification=ISO_19115)
    public static final Classification SECRET = new Classification("SECRET");

    /**
     * Of the highest secrecy.
     */
    @UML(identifier="topsecret", obligation=CONDITIONAL, specification=ISO_19115)
    public static final Classification TOP_SECRET = new Classification("TOP_SECRET");

    /**
     * Constructs an enum with the given name. The new enum is
     * automatically added to the list returned by {@link #values}.
     *
     * @param name The enum name. This name must not be in use by an other enum of this type.
     */
    public Classification(final String name) {
        super(name, VALUES);
    }

    /**
     * Returns the list of {@code Classification}s.
     */
    public static Classification[] values() {
        synchronized (VALUES) {
            return (Classification[]) VALUES.toArray(new Classification[VALUES.size()]);
        }
    }

    /**
     * Returns the list of enumerations of the same kind than this enum.
     */
    public /*{Classification}*/ CodeList[] family() {
        return values();
    }
}