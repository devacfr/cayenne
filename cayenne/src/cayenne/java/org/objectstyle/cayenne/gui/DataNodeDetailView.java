/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.cayenne.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DataSourceInfo;
import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.gui.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.gui.event.DataNodeDisplayListener;
import org.objectstyle.cayenne.gui.event.DataNodeEvent;
import org.objectstyle.cayenne.gui.event.Mediator;
import org.objectstyle.cayenne.gui.util.FileSystemViewDecorator;
import org.objectstyle.cayenne.gui.util.PreferenceField;
import org.objectstyle.cayenne.gui.util.SaveHandler;
import org.objectstyle.cayenne.util.Preferences;
import org.objectstyle.cayenne.util.Util;

/** 
 * Detail view of the DataNode and DataSourceInfo.
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public class DataNodeDetailView
    extends JPanel
    implements DocumentListener, ActionListener, DataNodeDisplayListener {
    static Logger logObj = Logger.getLogger(DataNodeDetailView.class.getName());

    Mediator mediator;
    // The node currently being edited on this screen.
    DataNode node;

    JLabel nameLabel;
    JTextField name;
    String oldName;

    JLabel locationLabel;
    JTextField location;
    JButton fileBtn;

    JLabel factoryLabel;
    JComboBox factory;

    JLabel adapterLabel;
    JComboBox adapter;

    JLabel userNameLabel;
    PreferenceField userName;
    JLabel passwordLabel;
    JPasswordField password;
    JLabel driverLabel;
    PreferenceField driver;
    JLabel urlLabel;
    PreferenceField url;
    JLabel minConnectionsLabel;
    // FIXME!!! Need to restrict only to numbers
    JTextField minConnections;
    JLabel maxConnectionsLabel;
    // FIXME!!! Need to restrict only to numbers
    JTextField maxConnections;

    /** Cludge to prevent marking domain as dirty during initial load. */
    private boolean ignoreChange;

    public DataNodeDetailView(Mediator temp_mediator) {
        super();
        mediator = temp_mediator;
        mediator.addDataNodeDisplayListener(this);
        // Create and layout components
        init();

        // Add listeners
        location.getDocument().addDocumentListener(this);
        name.getDocument().addDocumentListener(this);
        userName.getDocument().addDocumentListener(this);
        password.getDocument().addDocumentListener(this);
        driver.getDocument().addDocumentListener(this);
        url.getDocument().addDocumentListener(this);
        minConnections.getDocument().addDocumentListener(this);
        maxConnections.getDocument().addDocumentListener(this);
        factory.addActionListener(this);
        adapter.addActionListener(this);
        fileBtn.addActionListener(this);
    }

    private void init() {
        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 100;
        constraints.weighty = 50;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = GridBagConstraints.RELATIVE;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;

        nameLabel = new JLabel("Data node name: ");
        name = new JTextField(20);
        locationLabel = new JLabel("Location: ");
        location = new JTextField(25);
        fileBtn = new JButton("...");
        factoryLabel = new JLabel("Data source factory:");
        factory = new JComboBox();
        factory.setEditable(true);
        DefaultComboBoxModel model =
            new DefaultComboBoxModel(
                new String[] {
                    DataSourceFactory.JNDI_FACTORY,
                    DataSourceFactory.DIRECT_FACTORY });
        factory.setModel(model);
        factory.setSelectedIndex(-1);

        adapterLabel = new JLabel("DB adapter:");
        adapter = new JComboBox();
        adapter.setEditable(true);
        String[] arr1 =
            { DbAdapter.JDBC, DbAdapter.SYBASE, DbAdapter.MYSQL, DbAdapter.ORACLE
            /* [DISABLE POSTGRES DATA] ,DbAdapter.POSTGRES */
        };
        model = new DefaultComboBoxModel(arr1);
        adapter.setModel(model);
        adapter.setSelectedIndex(-1);

        JPanel fileChooser = this.formatFileChooser(location, fileBtn);

        Component[] left_comp = new Component[4];
        left_comp[0] = nameLabel;
        left_comp[1] = factoryLabel;
        left_comp[2] = locationLabel;
        left_comp[3] = adapterLabel;

        Component[] right_comp = new Component[4];
        right_comp[0] = name;
        right_comp[1] = factory;
        right_comp[2] = fileChooser;
        right_comp[3] = adapter;

        JPanel temp = PanelFactory.createForm(left_comp, right_comp, 5, 5, 5, 5);
        add(temp, constraints);
        location.setEditable(false);
        fileBtn.setVisible(false);

        userNameLabel = new JLabel("User name: ");
        userName = new PreferenceField(Preferences.USER_NAME);
        userName.addActionListener(this);
        passwordLabel = new JLabel("Password: ");
        password = new JPasswordField(20);
        driverLabel = new JLabel("Driver class: ");
        driver = new PreferenceField(Preferences.JDBC_DRIVER);
        driver.addActionListener(this);
        urlLabel = new JLabel("Database URL: ");
        url = new PreferenceField(Preferences.DB_URL);
        url.addActionListener(this);
        minConnectionsLabel = new JLabel("Min connections: ");
        minConnections = new JTextField(5);
        maxConnectionsLabel = new JLabel("Max connections: ");
        maxConnections = new JTextField(5);

        left_comp = new Component[6];
        left_comp[0] = userNameLabel;
        left_comp[1] = passwordLabel;
        left_comp[2] = driverLabel;
        left_comp[3] = urlLabel;
        left_comp[4] = minConnectionsLabel;
        left_comp[5] = maxConnectionsLabel;

        right_comp = new Component[6];
        right_comp[0] = userName;
        right_comp[1] = password;
        right_comp[2] = driver;
        right_comp[3] = url;
        right_comp[4] = minConnections;
        right_comp[5] = maxConnections;

        temp = PanelFactory.createForm(left_comp, right_comp, 5, 5, 5, 5);
        temp.setBorder(BorderFactory.createTitledBorder("Data Source Info"));
        constraints.gridheight = 2;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = GridBagConstraints.REMAINDER;
        add(temp, constraints);

    }

    private JPanel formatFileChooser(JTextField fld, JButton btn) {
        JPanel panel = new JPanel();

        panel.setLayout(new BorderLayout());
        panel.add(fld, BorderLayout.CENTER);
        panel.add(btn, BorderLayout.EAST);

        return panel;
    }

    public void insertUpdate(DocumentEvent e) {
        textFieldChanged(e);
    }
    public void changedUpdate(DocumentEvent e) {
        textFieldChanged(e);
    }
    public void removeUpdate(DocumentEvent e) {
        textFieldChanged(e);
    }

    private void textFieldChanged(DocumentEvent e) {
        if (ignoreChange || node == null) {
            return;
        }

        GuiDataSource src = (GuiDataSource) node.getDataSource();
        DataSourceInfo info = src.getDataSourceInfo();

        if (e.getDocument() == name.getDocument()) {

            String new_name = name.getText();
            // If name hasn't changed, do nothing
            if (oldName != null && new_name.equals(oldName))
                return;
            node.setName(new_name);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node, oldName));
            oldName = new_name;

        } else if (e.getDocument() == location.getDocument()) {

            if (node.getDataSourceLocation() != null
                && node.getDataSourceLocation().equals(location.getText()))
                return;
            node.setDataSourceLocation(location.getText());
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        } else if (e.getDocument() == userName.getDocument()) {

            String nameStr =
                (userName.getText().trim().length() > 0)
                    ? userName.getText().trim()
                    : null;
            info.setUserName(nameStr);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        } else if (e.getDocument() == driver.getDocument()) {

            String driverStr =
                (driver.getText().trim().length() > 0) ? driver.getText().trim() : null;
            info.setJdbcDriver(driverStr);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        } else if (e.getDocument() == url.getDocument()) {

            String urlStr =
                (url.getText().trim().length() > 0) ? url.getText().trim() : null;
            info.setDataSourceUrl(urlStr);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        } else if (e.getDocument() == password.getDocument()) {

            char[] pwd = password.getPassword();
            String pwdStr = (pwd != null && pwd.length > 0) ? new String(pwd) : null;

            info.setPassword(pwdStr);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
        } else if (e.getDocument() == minConnections.getDocument()) {

            if (minConnections.getText().trim().length() > 0)
                info.setMinConnections(Integer.parseInt(minConnections.getText()));
            else
                info.setMinConnections(0);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
        } else if (e.getDocument() == maxConnections.getDocument()) {

            if (maxConnections.getText().trim().length() > 0)
                info.setMaxConnections(Integer.parseInt(maxConnections.getText()));
            else
                info.setMaxConnections(0);
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
        }

    }

    public void actionPerformed(ActionEvent e) {
        if (ignoreChange || node == null) {
            return;
        }

        Object src = e.getSource();
        DataSourceInfo info = ((GuiDataSource) node.getDataSource()).getDataSourceInfo();

        DataNode aNode = mediator.getCurrentDataNode();

        // node factory changed
        if (src == factory) {
            String ele = (String) factory.getModel().getSelectedItem();
            if (ele != null && ele.trim().length() > 0) {
                if (ele.equals(DataSourceFactory.DIRECT_FACTORY)) {
                    fileBtn.setVisible(true);
                    location.setEditable(false);
                } else {
                    fileBtn.setVisible(false);
                    location.setEditable(true);
                }

                if (!ele.equals(aNode.getDataSourceFactory())) {
                    aNode.setDataSourceFactory(ele);
                    mediator.setDirty(true);
                }
            } else {
                if (aNode.getDataSourceFactory() != null) {
                    aNode.setDataSourceFactory(null);
                    mediator.setDirty(true);
                }
            }

        } else if (src == adapter) {
            // DBAdapter changed
            String adapterName = (String) adapter.getModel().getSelectedItem();
            String currentName =
                (node.getAdapter() != null)
                    ? node.getAdapter().getClass().getName()
                    : null;

         //   if (!Util.nullSafeEquals(currentName, adapterName)) {
            	// instantiate new adapter if needed
                DbAdapter newAdapter = null;
                if (adapterName != null && adapterName.trim().length() > 0) {
                    try {
                        newAdapter =
                            (DbAdapter) Class
                                .forName(adapterName)
                                .getDeclaredConstructors()[0]
                                .newInstance(
                                new Object[0]);
                    } catch (Exception ex) {
                        logObj.warn("Error.", ex);
                        adapter.setSelectedIndex(-1);
                        return;
                    }
                }

                mediator.getCurrentDataNode().setAdapter(newAdapter);
                mediator.setDirty(true);
         //   }
        } else if (src == fileBtn) {
            // "select node location" clicked
            selectNodeLocation();
        } else if (src == driver) {
            ignoreChange = true;
            driver.storePreferences();
            ignoreChange = false;
        } else if (src == url) {
            ignoreChange = true;
            url.storePreferences();
            ignoreChange = false;
        } else if (src == userName) {
            ignoreChange = true;
            userName.storePreferences();
            ignoreChange = false;
        }
    }

    /**
     * Handles changing file name of the data node configuration.
     */
    protected void selectNodeLocation() {
        SaveHandler saveHandler = new SaveHandler(mediator);
        String oldLocation = node.getDataSourceLocation();

        String projDirStr = mediator.getConfig().getProjDir();
        File projDir = (projDirStr != null) ? new File(projDirStr) : null;

        try {
            // don't allow changes on unsaved project,
            // unless this node was just recently added and doesn't have 
            // a location yet.
            if (oldLocation != null && !saveHandler.shouldProceed()) {
                return;
            }

            FileSystemViewDecorator fileView = new FileSystemViewDecorator(projDir);
            JFileChooser fc = new JFileChooser(fileView);
            fc.setDialogType(JFileChooser.SAVE_DIALOG);
            fc.setDialogTitle("Data Node Location");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

            if (projDir != null) {
                fc.setCurrentDirectory(projDir);
            }

            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File file = fc.getSelectedFile();

            // Determine and set new data map location
            String newFileLocation = file.getAbsolutePath();

            // If it is set, use path striped of proj dir and following separator
            // If proj dir not set, use absolute location.
            String relLocation =
                (projDirStr == null)
                    ? newFileLocation
                    : newFileLocation.substring(projDirStr.length() + 1);

            if (relLocation.equals(node.getDataSourceLocation())) {
                return;
            }

            // Create new file
            if (!file.exists()) {
                file.createNewFile();
            }

            node.setDataSourceLocation(relLocation);
            location.setText(relLocation);
        } catch (IOException ioex) {
            ErrorDebugDialog.guiWarning(ioex, "Error renaming map.");
            return;
        } catch (Throwable th) {
            ErrorDebugDialog.guiException(th);
            return;
        }

        // Map location changed
        mediator.fireDataNodeEvent(new DataNodeEvent(this, node));

        // remove old location
        if (oldLocation != null) {
            // save project only if the node file already exists
            saveHandler.saveProject();

            File oldFile =
                (projDir != null)
                    ? new File(projDir, oldLocation)
                    : new File(oldLocation);
            if (!oldFile.delete()) {
                logObj.info("Can't delete old file: " + oldFile);
            }
        }
    }

    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
        node = e.getDataNode();
        if (null == node) {
            return;
        }
        GuiDataSource src = (GuiDataSource) node.getDataSource();
        oldName = node.getName();
        ignoreChange = true;
        name.setText(oldName);
        populateFactory(node.getDataSourceFactory());
        DbAdapter adapter = node.getAdapter();
        if (null != adapter)
            populateDbAdapter(adapter.getClass().getName().trim());
        else
            populateDbAdapter("");
        DataSourceInfo info = src.getDataSourceInfo();
        populateDataSourceInfo(info);
        // Must be last in order not to be reset when data src factory is set.
        location.setText(node.getDataSourceLocation());
        ignoreChange = false;
    }

    private void populateDbAdapter(String selected_class) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) adapter.getModel();
        if (selected_class != null && selected_class.length() > 0) {
            boolean found = false;
            for (int i = 0; i < model.getSize(); i++) {
                String ele = (String) model.getElementAt(i);
                if (ele.equals(selected_class)) {
                    model.setSelectedItem(ele);
                    found = true;
                    break;
                }
            }

            if (!found) {
                model.addElement(selected_class);
                model.setSelectedItem(selected_class);
            }
        }
    }

    private void populateFactory(String selected_class) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) factory.getModel();
        if (selected_class != null && selected_class.length() > 0) {
            boolean found = false;
            for (int i = 0; i < model.getSize(); i++) {
                String ele = (String) model.getElementAt(i);
                if (ele.equals(selected_class)) {
                    model.setSelectedItem(ele);
                    found = true;
                    // If direct connection, 
                    // show File button and disable text field.
                    // Otherwise hide File button and enable text field.
                    if (selected_class.equals(DataSourceFactory.DIRECT_FACTORY)) {
                        fileBtn.setVisible(true);
                        location.setEditable(false);
                    } else {
                        fileBtn.setVisible(true);
                        location.setEditable(true);
                    }

                    break;
                }
            }

            if (!found) {
                model.addElement(selected_class);
                model.setSelectedItem(selected_class);
            }
        } // End if there is factory to select
        else
            model.setSelectedItem(null);
    }

    private void populateDataSourceInfo(DataSourceInfo info) {
        userName.setText(info.getUserName());
        password.setText(info.getPassword());
        driver.setText(info.getJdbcDriver());
        url.setText(info.getDataSourceUrl());
        minConnections.setText(String.valueOf(info.getMinConnections()));
        maxConnections.setText(String.valueOf(info.getMaxConnections()));
    } //end populateDataSourceInfo()
} // End class DataNodeDetailView
