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
package org.opengis.test.runner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Vector;
import java.util.Arrays;
import java.util.prefs.Preferences;
import java.util.concurrent.ExecutionException;

import java.awt.Desktop;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.SwingWorker;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumnModel;
import javax.swing.table.DefaultTableModel;


/**
 * The main frame of the test runner.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 3.1
 * @since   3.1
 */
@SuppressWarnings("serial")
final class SwingFrame extends JFrame implements Runnable, ActionListener, ListSelectionListener {
    /**
     * The preference key for the directory in which to select JAR files.
     */
    private static final String JAR_DIRECTORY_KEY = "jar.directory";

    /**
     * The desktop for browse operations, or {@code null} if unsupported.
     */
    private final Desktop desktop;

    /**
     * Labels used for rendering information from {@link ImplementationManifest}.
     *
     * @see #setManifest(ImplementationManifest)
     */
    private final JLabel title, vendor, version, vendorID, url, specification, specVersion, specVendor;

    /**
     * The table showing the results.
     */
    private final SwingResultTableModel results;

    /**
     * Labels used for rendering information about the selected test.
     *
     * @see #setDetails(ReportEntry)
     */
    private final JLabel testName;

    /**
     * The factories used for the test case reported in the "details" tab.
     */
    private final DefaultTableModel factories;

    /**
     * Where to report stack trace.
     */
    private final JTextArea exception;

    /**
     * The object to use for running the tests.
     */
    private final Runner runner;

    /**
     * The test report which is currently shown in the "details" tab, or {@code null} if none.
     */
    private ReportEntry currentReport;

    /**
     * Creates a new frame.
     */
    SwingFrame() {
        super("GeoAPI conformance tests");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800, 600); // If width is modified, please adjust column preferred widths below.
        setLocationByPlatform(true);
        desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

        add(new SwingPanelBuilder().createManifestPane(
                title         = new JLabel(),
                version       = new JLabel(),
                vendor        = new JLabel(),
                vendorID      = new JLabel(),
                url           = new JLabel(),
                specification = new JLabel(),
                specVersion   = new JLabel(),
                specVendor    = new JLabel()), BorderLayout.NORTH);

        runner = new Runner();
        results = new SwingResultTableModel(runner);
        final JTable table = new JTable(results);
        table.setDefaultRenderer(String.class, new SwingResultCellRenderer());
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getSelectionModel().addListSelectionListener(this);
        final TableColumnModel columns = table.getColumnModel();
        columns.getColumn(SwingResultTableModel.CLASS_COLUMN)  .setPreferredWidth(125);
        columns.getColumn(SwingResultTableModel.METHOD_COLUMN) .setPreferredWidth(175);
        columns.getColumn(SwingResultTableModel.RESULT_COLUMN) .setPreferredWidth( 40);
        columns.getColumn(SwingResultTableModel.MESSAGE_COLUMN).setPreferredWidth(250); // Take all remaining space.

        final JButton viewJavadoc = new JButton(new ImageIcon(SwingFrame.class.getResource("documentinfo.png")));
        viewJavadoc.setEnabled(desktop != null && desktop.isSupported(Desktop.Action.BROWSE));
        viewJavadoc.setToolTipText("View javadoc for this test");
        viewJavadoc.addActionListener(this);

        factories = new DefaultTableModel();
        factories.setColumnIdentifiers(new String[] {
            // See the javadoc of ReportEntry.factories
            "Category", "Implementation", "Vendor", "Authority"
        });

        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Tests", new JScrollPane(table));
        tabs.addTab("Details", new SwingPanelBuilder().createDetailsPane(
                exception = new JTextArea(),
                testName  = new JLabel(), viewJavadoc,
                new JTable(factories)));
        add(tabs, BorderLayout.CENTER);
    }

    /**
     * Opens the file chooser dialog box for selecting JAR files. This method remember the
     * directory selected by the user last time this method was executed. This method is
     * invoked from the {@link Main} class.
     */
    @Override
    public void run() {
        final Preferences prefs = Preferences.userNodeForPackage(org.opengis.test.TestCase.class);
        final String directory = prefs.get(JAR_DIRECTORY_KEY, null);
        final JFileChooser chooser = new JFileChooser(directory != null ? new File(directory) : null);
        chooser.setDialogTitle("Select a GeoAPI implementation");
        chooser.setFileFilter(new FileNameExtensionFilter("Java Archive Files", "jar"));
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            prefs.put(JAR_DIRECTORY_KEY, chooser.getCurrentDirectory().getPath());
            new Loader(chooser.getSelectedFiles()).execute();
        }
    }

    /**
     * Sets the implementation identification.
     */
    private void setManifest(final ImplementationManifest manifest) {
        title        .setText(manifest != null ? manifest.title         : null);
        version      .setText(manifest != null ? manifest.version       : null);
        vendor       .setText(manifest != null ? manifest.vendor        : null);
        vendorID     .setText(manifest != null ? manifest.vendorID      : null);
        url          .setText(manifest != null ? manifest.url           : null);
        specification.setText(manifest != null ? manifest.specification : null);
        specVersion  .setText(manifest != null ? manifest.specVersion   : null);
        specVendor   .setText(manifest != null ? manifest.specVendor    : null);
    }

    /**
     * Updates the content of the "Details" pane with information relative to the given entry.
     * A {@code null} entry clears the "Details" pane.
     */
    private void setDetails(final ReportEntry entry) {
        String className  = null;
        String methodName = null;
        String stacktrace = null;
        @SuppressWarnings("unchecked")
        final Vector<Vector<String>> factoryTableContent = factories.getDataVector();
        if (entry == null) {
            factoryTableContent.clear();
        } else {
            className  = entry.className;
            methodName = entry.methodName;
            switch (entry.status) {
                case FAILURE: {
                    if (entry.exception != null) {
                        final StringWriter buffer = new StringWriter();
                        final PrintWriter printer = new PrintWriter(buffer);
                        entry.exception.printStackTrace(printer);
                        printer.flush();
                        stacktrace = buffer.toString();
                    }
                    break;
                }
            }
            final List<String[]> newFactoryContent = entry.factories;
            final int numFactories = newFactoryContent.size();
            factoryTableContent.setSize(numFactories);
            for (int i=0; i<numFactories; i++) {
                final String[] values = newFactoryContent.get(i);
                Vector<String> row = factoryTableContent.get(i);
                if (row == null) {
                    row = new Vector<String>(values.length);
                    factoryTableContent.set(i, row);
                }
                row.clear();
                row.addAll(Arrays.asList(values));
            }
        }
        factories .fireTableDataChanged();
        testName  .setText(className + '.' + methodName);
        exception .setText(stacktrace);
        exception .setEnabled(stacktrace != null);
        exception .setCaretPosition(0);
        currentReport = entry;
    }

    /**
     * Invoked when the user clicked on a new row in the table showing test results.
     * This method updates the "Details" tab with information relative to the test
     * in the selected row.
     */
    @Override
    public void valueChanged(final ListSelectionEvent event) {
        if (!event.getValueIsAdjusting()) {
            final int row = ((ListSelectionModel) event.getSource()).getMinSelectionIndex();
            if (row >= 0) {
                setDetails(results.getValueAt(row));
            }
        }
    }

    /**
     * Invoked when the user pressed the "View javadoc" button.
     */
    @Override
    public void actionPerformed(final ActionEvent event) {
        if (currentReport != null) try {
            desktop.browse(currentReport.getJavadocURL());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(SwingFrame.this, e.toString(),
                    "Can not open the browser", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * The worker for loading JAR files in background.
     */
    private final class Loader extends SwingWorker<Object,Object> {
        /**
         * The JAR files.
         */
        private final File[] files;

        /**
         * Creates a new worker which will loads the given JAR files.
         */
        Loader(final File[] files) {
            this.files = files;
        }

        /**
         * Loads the given JAR files and creates a class loader for running the tests.
         */
        @Override
        protected Object doInBackground() throws IOException {
            setManifest(ImplementationManifest.parse(files));
            Runner.setClassLoader(files);
            runner.run();
            return null;
        }

        /**
         * Invoked from the Swing thread when the task is over of failed.
         */
        @Override
        protected void done() {
            try {
                get();
            } catch (InterruptedException e) {
                // Should not happen at this point.
            } catch (ExecutionException e) {
                JOptionPane.showMessageDialog(SwingFrame.this,
                        "An error occurred while processing the JAR files: " + e.getCause(),
                        "Can not use the JAR files", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}