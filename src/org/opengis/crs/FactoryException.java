/*$************************************************************************************************
 **
 ** $Id$
 **
 ** $Source$
 **
 ** Copyright (C) 2003 Open GIS Consortium, Inc. All Rights Reserved. http://www.opengis.org/Legal/
 **
 *************************************************************************************************/
package org.opengis.crs;


/**
 * Thrown when a {@linkplain Factory factory} can't create an instance
 * of the requested object. It may be a failure to create a
 * {@linkplain org.opengis.crs.datum.Datum datum}, a
 * {@linkplain org.opengis.crs.cs.CoordinateSystem coordinate system}, a
 * {@linkplain org.opengis.crs.ReferenceSystem reference system} or a
 * {@linkplain org.opengis.crs.operation.CoordinateOperation coordinate operation}.
 *
 * If the failure is caused by an illegal authority code, then the actual exception should
 * be {@link NoSuchAuthorityCodeException}. Otherwise, if the failure is caused by some
 * error in the underlying database (e.g. {@link java.io.IOException} or
 * {@link java.sql.SQLException}), then this cause should be specified.
 *
 * @author <A HREF="http://www.opengis.org">OpenGIS&reg; consortium</A>
 * @version 1.0
 *
 * @see org.opengis.crs.operation.CoordinateOperationFactory
 */
public class FactoryException extends Exception {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -3414250034883898315L;

    /**
     * Construct an exception with no detail message.
     */
    public FactoryException() {
    }

    /**
     * Construct an exception with the specified detail message.
     *
     * @param  message The detail message. The detail message is saved
     *         for later retrieval by the {@link #getMessage()} method.
     */
    public FactoryException(String message) {
        super(message);
    }

    /**
     * Construct an exception with the specified cause. The detail message
     * is copied from the cause {@linkplain Exception#getLocalizedMessage
     * localized message}.
     *
     * @param  cause The cause for this exception. The cause is saved
     *         for later retrieval by the {@link #getCause()} method.
     */
    public FactoryException(Exception cause) {
        super(cause.getLocalizedMessage(), cause);
    }

    /**
     * Construct an exception with the specified detail message and cause.
     * The cause is the exception thrown in the underlying database
     * (e.g. {@link java.io.IOException} or {@link java.sql.SQLException}).
     *
     * @param  message The detail message. The detail message is saved
     *         for later retrieval by the {@link #getMessage()} method.
     * @param  cause The cause for this exception. The cause is saved
     *         for later retrieval by the {@link #getCause()} method.
     */
    public FactoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
