package org.objectstyle.cayenne.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.collections.ExtendedProperties;

import org.objectstyle.cayenne.conf.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.gui.event.*;
import org.objectstyle.cayenne.gui.util.*;
import org.objectstyle.cayenne.gui.datamap.*;



/** Window for the Cayenne Modeler.
  * Responsibilities include coordination of enabling/disabling of
  * menu and toolbar. */
public class Editor extends JFrame
implements ActionListener
, DomainDisplayListener, DataNodeDisplayListener, DataMapDisplayListener
, ObjEntityDisplayListener, DbEntityDisplayListener
{
	/** Directory for preferences in User home. */
	public static final String CAYENNE_PREF_DIR = "cayenne";
	/** Name of the preferences file in the CAYENNE_PREF_DIR.
	  * General standard for keys in the preferences:
	  * Use class name (optionally with the package name) and 
	  * the name of the field which uses this preference. */
	public static final String CAYENNE_PREF = ".preferences";
	/* Keys for the preference file. */
	/** The directory of the cayenne project edited last. */
	public static final String LAST_DIR = "Editor.lastProject";
	
    EditorView view;
    Mediator mediator;
    /** The object last selected in BrowseView. */
    Object context;

    JMenuBar  menuBar    = new JMenuBar();
    JMenu  fileMenu    = new JMenu("File");
    JMenuItem  createProjectMenu  = new JMenuItem("New Project");
    JMenuItem createDomainMenu = new JMenuItem("New Domain");
    JMenuItem createDataMapMenu = new JMenuItem("New Data Map");
    JMenuItem createDataSourceMenu= new JMenuItem("New Data Source");
    JMenuItem createObjEntityMenu = new JMenuItem("New Object Entity");
    JMenuItem createDbEntityMenu = new JMenuItem("New DB Entity");
    JMenuItem removeMenu = new JMenuItem("Remove");
    JMenuItem  openProjectMenu  = new JMenuItem("Open Project");
    JMenuItem  openDataMapMenu  = new JMenuItem("Open Data Map");
    JMenuItem  closeProjectMenu  = new JMenuItem("Close Project");
    JMenuItem saveMapMenu    = new JMenuItem("Save Current Map");
    JMenuItem saveMapAsMenu    = new JMenuItem("Save Current Map As");
    JMenuItem saveProjectMenu    = new JMenuItem("Save Project");
    JMenuItem saveAllMenu   = new JMenuItem("Save All");
    JMenuItem  exitMenu    = new JMenuItem("Exit");
    JMenu  toolMenu   = new JMenu("Tools");
    JMenuItem importDbMenu  = new JMenuItem("Reverse engineer database");
    JMenuItem generateMenu  = new JMenuItem("Generate Classes");

    //Create a file chooser
    final JFileChooser fileChooser   = new JFileChooser();
    XmlFilter xmlFilter    			 = new XmlFilter();
    
    ExtendedProperties pref = new ExtendedProperties();

	private static Editor frame;

    private Editor() {
        super("Cayenne Modeler");

        init();
        loadPreferences();
		disableMenu();

        createProjectMenu.addActionListener(this);
        createDomainMenu.addActionListener(this);
        createDataMapMenu.addActionListener(this);
        createDataSourceMenu.addActionListener(this);
        createObjEntityMenu.addActionListener(this);
        createDbEntityMenu.addActionListener(this);
        removeMenu.addActionListener(this);
        openProjectMenu.addActionListener(this);
        openDataMapMenu.addActionListener(this);
        closeProjectMenu.addActionListener(this);
        saveMapMenu.addActionListener(this);
        saveProjectMenu.addActionListener(this);
        saveMapAsMenu.addActionListener(this);
        saveAllMenu.addActionListener(this);
        exitMenu.addActionListener(this);

        importDbMenu.addActionListener(this);
        generateMenu.addActionListener(this);

	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	setSize(650, 550);
    	
    	this.addWindowListener(new WindowAdapter() {
    		public void windowClosing(WindowEvent e) {
    			storePreferences();
    		}
    	});
    }

	/** Singleton implementation of getting Editor window. */
	public static Editor getFrame() {
		if (null == frame) {
			frame = new Editor();
		}
		return frame;
	}

	public ExtendedProperties getPreferences() { return pref; }

	private void loadPreferences() {
		String home_dir = System.getProperty("user.home");
		if (null == home_dir) {
			JOptionPane.showMessageDialog(this
							, "User home directory is not specified. "
							+ " Loading from current directory");
			home_dir = "";
		}
		String pref_dir = home_dir + File.separator + Editor.CAYENNE_PREF_DIR;
		System.out.println("Editor::loadPreferences(), pref dir path is " + pref_dir);
		File pref_dir_file = new File(pref_dir);
		try {
			if (!pref_dir_file.exists()) {
				if (false == pref_dir_file.mkdir()) {
					JOptionPane.showMessageDialog(this
							, "Error creating preferences directory. ");
					return;
				}
			}
			String pref_file_name = pref_dir + File.separator + Editor.CAYENNE_PREF;
			File pref_file = new File(pref_file_name);
			if (!pref_file.exists()) {
				if (false == pref_file.createNewFile()) {
					JOptionPane.showMessageDialog(this
							, "Error creating preferences file. ");
					return;
				}
			}
			pref.load(new FileInputStream(pref_file));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this
							, "Error loading preferences. Preferences ignored. ");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/** Store preferences (when the Editor window closes down). 
	  * Preferences stored to User Home\cayenne\.preferences file. */
	private void storePreferences() {
		System.out.println("Storing preferences");
		String home_dir = System.getProperty("user.home");
		if (null == home_dir)
			home_dir = "";
		String pref_dir = home_dir + File.separator + Editor.CAYENNE_PREF_DIR 
						+ File.separator + Editor.CAYENNE_PREF;
		File pref_file = new File(pref_dir);
		try {
			if (!pref_file.exists()) {
				System.out.println("Cannot save preferences - file " 
									+ pref_dir + " does not exist");
				return;
			}
			pref.save(new FileOutputStream(pref_file), "");
		} catch (IOException e) {
			System.out.println("Error saving preferences: " + e.getMessage());
			e.printStackTrace();
		}
	}

    private void init() {
        getContentPane().setLayout(new BorderLayout());

        // Setup menu bar
        setJMenuBar(menuBar);
        menuBar.add(fileMenu);
        menuBar.add(toolMenu);

        fileMenu.add(createProjectMenu);
        fileMenu.add(createDomainMenu);
        fileMenu.add(createDataMapMenu);
        fileMenu.add(createDataSourceMenu);
        fileMenu.add(createObjEntityMenu);
        fileMenu.add(createDbEntityMenu);
        fileMenu.addSeparator();
        fileMenu.add(removeMenu);
        fileMenu.addSeparator();
        fileMenu.add(openProjectMenu);
        fileMenu.add(openDataMapMenu);
        fileMenu.addSeparator();
        fileMenu.add(closeProjectMenu);
        fileMenu.add(saveMapMenu);
        fileMenu.add(saveMapAsMenu);
        fileMenu.add(saveProjectMenu);
        fileMenu.add(saveAllMenu);
        fileMenu.addSeparator();
        fileMenu.add(exitMenu);

        toolMenu.add(importDbMenu);
        toolMenu.add(generateMenu);
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == createProjectMenu) {
            createProject();
        } else if (src == openProjectMenu) {
            openProject();
        } else if (src == closeProjectMenu) {
        	closeProject();
        } else if (src == saveMapAsMenu) {
            saveMapAs(mediator.getCurrentDataMap());
        } else if (src == createProjectMenu) {
            createProject();
        } else if (src == createDomainMenu) {
            createDomain();
        } else if (src == createDataMapMenu) {
        	mediator.addDataMap(this, new DataMap(DataMapWrapper.sessionUniqueDomainName()));
    	} else if (src == createDataSourceMenu) {
    		createDataNode();
        } else if (src == createObjEntityMenu) {
        	createObjEntity();
        } else if (src == createDbEntityMenu) {
        	createDbEntity();
        } else if (src == removeMenu) {
        	remove();
        } else if (src == saveMapMenu) {
            saveDataMap(mediator.getCurrentDataMap());
        } else if (src == saveProjectMenu) {
            saveProject();
        } else if (src == saveAllMenu) {
            saveAll();
        } else if (src == importDbMenu) {
            importDb();
        } else if (src == generateMenu) {
            generateClasses();
        } else if (src == exitMenu) {
        	storePreferences();
            setVisible(false);
            dispose();
            System.exit(0);
        }
    }

	private void remove()
	{
		if (context instanceof DataDomain) {
		} else if (context instanceof DataNode) {
		} else if (context instanceof DataMap) {
			mediator.removeDataMap(this, (DataMap)context);
		} else if (context instanceof DbEntity) {
			mediator.removeDbEntity(this, (DbEntity)context);
		} else if (context instanceof ObjEntity) {
			mediator.removeObjEntity(this, (ObjEntity)context);
		}
		
	}

	private void generateClasses() {
		GenerateClassDialog dialog;
		dialog = new GenerateClassDialog(this, mediator);
		dialog.show();
		dialog.dispose();
	}


	private void closeProject()
	{
        getContentPane().remove(view);
        view = null;
        mediator = null;
        repaint();
        disableMenu();
        removeMenu.setText("Remove");
	}

	private void importDb() {
        DataSourceInfo dsi = new DataSourceInfo();
        Connection conn = null;
        // Get connection
        while (conn == null) {
	        InteractiveLogin loginObj = InteractiveLogin.getGuiLoginObject(dsi);
	        loginObj.collectLoginInfo();
	        // connect
	        dsi = loginObj.getDataSrcInfo();
	        if (null == dsi) {
	        	return;
	        }
	        try {
		        Driver driver = (Driver)Class.forName(dsi.getJdbcDriver()).newInstance();
		        conn = DriverManager.getConnection(
		              					dsi.getDataSourceUrl(),
		                   				dsi.getUserName(),
		                   				dsi.getPassword());
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				SQLException ex = e.getNextException();
				if (ex != null) {
					System.out.println(ex.getMessage());
				}
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error Connecting to the Database"
							, JOptionPane.ERROR_MESSAGE);
				continue;
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error loading driver"
							, JOptionPane.ERROR_MESSAGE);
				continue;
			}
		}// End while()
		
		ArrayList schemas;
		DbLoader loader = new DbLoader(conn);
		try {
			schemas = loader.getSchemas();
		} catch (SQLException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error loading schemas"
							, JOptionPane.ERROR_MESSAGE);
				return;
		}
		ChooseSchemaDialog dialog = new ChooseSchemaDialog(schemas);
		dialog.show();
		if (dialog.getChoice() == ChooseSchemaDialog.CANCEL)
			return;
		String schema_name = dialog.getSchemaName();
		if (schema_name != null && schema_name.length() == 0)
			schema_name = null;
		DataMap map;
		try {
			map = loader.createDataMapFromDB(schema_name);
		} catch (SQLException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this
							, e.getMessage(), "Error reverse engineering database"
							, JOptionPane.ERROR_MESSAGE);
				return;
		}
		System.out.println("Num of db entities in the map: " + map.getDbEntities().length);
	    mediator.fireDataMapEvent(new DataMapEvent(this, map, DataMapEvent.ADD));
	    mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(this, map
	    												, mediator.getCurrentDataDomain()));
	}


	private void createObjEntity() {
		ObjEntity entity = EntityWrapper.createObjEntity();
		mediator.getCurrentDataMap().addObjEntity(entity);
		mediator.fireObjEntityEvent(new EntityEvent(this, entity, EntityEvent.ADD));
		mediator.fireObjEntityDisplayEvent(
				new EntityDisplayEvent(this, entity
									, mediator.getCurrentDataMap()
									, mediator.getCurrentDataDomain()
									, mediator.getCurrentDataNode()));
	}

	private void createDbEntity() {
		DbEntity entity = EntityWrapper.createDbEntity();
		mediator.getCurrentDataMap().addDbEntity(entity);
		mediator.fireDbEntityEvent(new EntityEvent(this, entity, EntityEvent.ADD));
		mediator.fireDbEntityDisplayEvent(
				new EntityDisplayEvent(this, entity
									, mediator.getCurrentDataMap()
									, mediator.getCurrentDataDomain()
									, mediator.getCurrentDataNode()));
	}

    private void createProject() {
       	String init_dir = (String)getPreferences().getProperty(LAST_DIR);
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            fileChooser.setFileFilter(new ProjFileFilter());
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogTitle("Choose new project directory");
            if (null != init_dir) {
            	File init_dir_file = new File(init_dir);
            	if (init_dir_file.exists())
            		fileChooser.setCurrentDirectory(init_dir_file);
            }
            int ret_code = fileChooser.showSaveDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fileChooser.getSelectedFile();
			System.out.println("File path is " + file.getAbsolutePath());
            if (!file.exists())
            	file.createNewFile();
            // Save dir path to the preferences
            getPreferences().setProperty(LAST_DIR, file.getAbsolutePath());
            // Create project file (cayenne.xml)
            File proj_file = new File(file.getAbsolutePath() 
            							+ File.separator 
            							+ "cayenne.xml");
            if (!proj_file.exists())
            	proj_file.createNewFile();
			
			System.out.println("proj file path is " 
					+ proj_file.getAbsolutePath());
			
			FileWriter fw = new FileWriter(proj_file);
			PrintWriter pw = new PrintWriter(fw, true);
			pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			pw.println("<domains>");
			pw.println("</domains>");
			pw.flush();
			pw.close();
			fw.close();
            GuiConfiguration.initSharedConfig(proj_file);
            GuiConfiguration config = GuiConfiguration.getGuiConfig();
            Mediator mediator = Mediator.getMediator(config);
            project(mediator);
        } catch (Exception e) {
            System.out.println("Error loading project file, " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Open specified cayenne.xml file. */
    private void openProject() {
       	String init_dir = (String)getPreferences().getProperty(LAST_DIR);
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            fileChooser.setFileFilter(new ProjFileFilter());
            fileChooser.setDialogTitle("Choose project file (cayenne.xml)");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (null != init_dir) {
            	File init_dir_file = new File(init_dir);
            	if (init_dir_file.exists())
            		fileChooser.setCurrentDirectory(init_dir_file);
            }
            int ret_code = fileChooser.showOpenDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fileChooser.getSelectedFile();
            // Save dir path to the preferences
			getPreferences().setProperty(LAST_DIR, file.getParent());
			System.out.println("File path is " + file.getAbsolutePath());
			GuiConfiguration.initSharedConfig(file);
            GuiConfiguration config = GuiConfiguration.getGuiConfig();
            Mediator mediator = Mediator.getMediator(config);
            project(mediator);

        } catch (Exception e) {
            System.out.println("Error loading project file, " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void project(Mediator temp_mediator) {
        mediator = temp_mediator;
        
        view = new EditorView(mediator);
        getContentPane().add(view, BorderLayout.CENTER);
        
        mediator.addDomainDisplayListener(this);
        mediator.addDataNodeDisplayListener(this);
        mediator.addDataMapDisplayListener(this);
        mediator.addObjEntityDisplayListener(this);
        mediator.addDbEntityDisplayListener(this);
        createDomainMenu.setEnabled(true);
        closeProjectMenu.setEnabled(true);
        this.validate();
    }


	/** Save data map to a different location. 
	  * If there already exists proj tree, saves it under that tree.
	  * otherwise saves using absolute path. */
	private void saveMapAs(DataMap map) {
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            String proj_dir_str = mediator.getConfig().getProjDir();
            File proj_dir = null;
            if (proj_dir_str != null)
            	proj_dir = new File(proj_dir_str);
            JFileChooser fc;
            FileSystemViewDecorator file_view;
            file_view = new FileSystemViewDecorator(proj_dir);
            fc = new JFileChooser(file_view);
            fc.setFileFilter(xmlFilter);
            fc.setDialogType(JFileChooser.SAVE_DIALOG);
            fc.setDialogTitle("Save data map - " + map.getName());
            if (null != proj_dir)
            	fc.setCurrentDirectory(proj_dir);
            int ret_code = fc.showSaveDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fc.getSelectedFile();
			System.out.println("File path is " + file.getAbsolutePath());
            String old_loc = map.getLocation();
            // Get absolute path for old location
            if (null != proj_dir)
            	old_loc = proj_dir + File.separator + old_loc;
			// Create new file
			if (!file.exists())
				file.createNewFile();
			MapLoader saver = new MapLoaderImpl();
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			saver.storeDataMap(pw, map);
			pw.close();
			fw.close();
			// Determine and set new data map name
			String new_file_name = file.getName();
			String new_name;
			int index = new_file_name.indexOf(".");
			if (index >= 0)
				new_name = new_file_name.substring(0, index);
			else
				new_name = new_file_name;			
			map.setName(new_name);
			// Determine and set new data map location
			String new_file_location = file.getAbsolutePath();
			String relative_location;
			// If it is set, use path striped of proj dir and following separator
			// If proj dir not set, use absolute location.
			if (proj_dir_str == null)
			 	relative_location = new_file_location;
			else
				relative_location 
					= new_file_location.substring(proj_dir_str.length() + 1);
			map.setLocation(relative_location);
            // If data map already exists, delete old location after saving new
            if (null != old_loc) {
            	System.out.println("Old location is " + old_loc);
            	File old_loc_file = new File(old_loc);
            	if (old_loc_file.exists()) {
            		System.out.println("Deleting old file");
            		old_loc_file.delete();
            	}
            }
            // Map location changed - mark current domain dirty
			mediator.fireDataMapEvent(new DataMapEvent(this, map, DataMapEvent.CHANGE));

        } catch (Exception e) {
            System.out.println("Error loading project file, " + e.getMessage());
            e.printStackTrace();
        }
	}


	/** Save data node (DataSourceInfo) to a different location. 
	  * If there already exists proj tree, saves it under that tree.
	  * otherwise saves using absolute path. */
	private void saveNodeAs(DataNode node) {
		GuiDataSource src = (GuiDataSource)node.getDataSource();
        try {
            // Get the project file name (always cayenne.xml)
            File file = null;
            String proj_dir_str = mediator.getConfig().getProjDir();
            File proj_dir = null;
            if (proj_dir_str != null)
            	proj_dir = new File(proj_dir_str);
            JFileChooser fc;
            FileSystemViewDecorator file_view;
            file_view = new FileSystemViewDecorator(proj_dir);
            fc = new JFileChooser(file_view);
            fc.setFileFilter(xmlFilter);
            fc.setDialogType(JFileChooser.SAVE_DIALOG);
            fc.setDialogTitle("Save data node - " + node.getName());
            if (null != proj_dir)
            	fc.setCurrentDirectory(proj_dir);
            int ret_code = fc.showSaveDialog(this);
            if ( ret_code != JFileChooser.APPROVE_OPTION)
                return;
            file = fc.getSelectedFile();
			System.out.println("File path is " + file.getAbsolutePath());
            String old_loc = node.getDataSourceLocation();
            // Get absolute path for old location
            if (null != proj_dir)
            	old_loc = proj_dir + File.separator + old_loc;
			// Create new file
			if (!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			DomainHelper.storeDataNode(pw, src.getDataSourceInfo());
			pw.close();
			fw.close();
			// Determine and set new data map location
			String new_file_location = file.getAbsolutePath();
			String relative_location;
			// If it is set, use path striped of proj dir and following separator
			// If proj dir not set, use absolute location.
			if (proj_dir_str == null)
			 	relative_location = new_file_location;
			else
				relative_location 
					= new_file_location.substring(proj_dir_str.length() + 1);
			node.setDataSourceLocation(relative_location);
            // If data map already exists, delete old location after saving new
            if (null != old_loc) {
            	System.out.println("Old location is " + old_loc);
            	File old_loc_file = new File(old_loc);
            	if (old_loc_file.exists()) {
            		System.out.println("Deleting old file");
            		old_loc_file.delete();
            	}
            }
            // Map location changed - mark current domain dirty
			mediator.fireDataNodeEvent(new DataNodeEvent(this, node, DataNodeEvent.CHANGE));

        } catch (Exception e) {
            System.out.println("Error saving DataNode " + node.getName() +": " + e.getMessage());
            e.printStackTrace();
        }
	}
	
	private void save() {
		if (mediator.getCurrentDataMap() != null) {
			saveDataMap(mediator.getCurrentDataMap());
		} 
		else 
			saveProject();
	}

	private void saveAll() {
		Iterator iter = mediator.getDirtyDataMaps().iterator();
		while (iter.hasNext()) {
			DataMap map = (DataMap)iter.next();
			saveDataMap(map);
			iter.remove();
		}// End saving maps

		iter = mediator.getDirtyDataNodes().iterator();
		while (iter.hasNext()) {
			DataNode node = (DataNode)iter.next();
			System.out.println("Editor::saveAll(), node name " 
								+ node.getName() + ", factory " 
								+ node.getDataSourceFactory());
			// If using direct connection, save into separate file
			if (node.getDataSourceFactory().equals(DataSourceFactory.DIRECT_FACTORY)) {
				System.out.println("Editor::saveAll(), saving node name " 
								+ node.getName());
				saveDataNode(node);
			}
			iter.remove();
		}// End saving DataNode-s
		
		saveProject();
	}
	
	private void saveProject() {
		File file = mediator.getConfig().getProjFile();
		System.out.println("Saving project to " + file.getAbsolutePath());
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file);
			DomainHelper.storeDomains(new PrintWriter(fw), mediator.getDomains());
			fw.flush();
			fw.close();
			mediator.getDirtyDomains().clear();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/** Save data map to the file. */
	private void saveDataMap(DataMap map) {
		try {
            File file = null;
            String proj_dir_str = mediator.getConfig().getProjDir();
			file = new File(proj_dir_str + File.separator + map.getLocation());			
			if (!file.exists()) {
				saveMapAs(map);
				return;
			}
			MapLoader saver = new MapLoaderImpl();
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			saver.storeDataMap(pw, map);
			pw.close();
			fw.close();
		} catch (Exception e) {}
	}
	

	/** Save data source info if data source factory is DIRECT_FACTORY. */
	private void saveDataNode(DataNode node) {
		try {
            File file = null;
            String proj_dir_str = mediator.getConfig().getProjDir();
			file = new File(proj_dir_str + File.separator + node.getDataSourceLocation());
			if (!file.exists()) {
				System.out.println("Editor::saveDataNode(), "
									+"calling save as for node name " 
									+ node.getName());
				saveNodeAs(node);
				return;
			}
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			GuiDataSource src = (GuiDataSource)node.getDataSource();
			System.out.println("Editor::saveDataNode(), node name " 
								+ node.getName());
			DomainHelper.storeDataNode(pw, src.getDataSourceInfo());
			pw.close();
			fw.close();
		} catch (Exception e) {
            System.out.println("SaveDataNode(), Error saving DataNode " 
            				+ node.getName()  +": " + e.getMessage());
            e.printStackTrace();
		}
	}

	private void createDomain() {
		DataDomain domain = new DataDomain(DataDomainWrapper.sessionUniqueDomainName());
		mediator.getConfig().addDomain(domain);
		mediator.fireDomainEvent(new DomainEvent(this, domain, DomainEvent.ADD));
		mediator.fireDomainDisplayEvent(new DomainDisplayEvent(this, domain));
	}

	/** Creates a new data node. Data node may consist of two pieces of information:
	  * 1. Name/location
	  * 2. Database url/uid/password (for direct connection to DB).
	  * First piece of info is stored directly into the cayenne.xml. 
	  * Second piece of data should be stored in the separate file
	  * if the factory requires it. */
	private void createDataNode() {
		DataNode node = new DataNode(NameGenerator.getDataNodeName());
		GuiDataSource src;
		src = new GuiDataSource(new DataSourceInfo());
		node.setDataSource(src);
		DataDomain domain = mediator.getCurrentDataDomain();
		domain.addNode(node);
		mediator.fireDataNodeEvent(new DataNodeEvent(this, node, DataNodeEvent.ADD));
		mediator.fireDataNodeDisplayEvent(new DataNodeDisplayEvent(this, domain, node));
	}


	public void currentDomainChanged(DomainDisplayEvent e){
		enableDomainMenu();
		removeMenu.setText("Remove Domain");
		context = e.getDomain();
	}

	public void currentDataNodeChanged(DataNodeDisplayEvent e){
		enableDomainMenu();
		removeMenu.setText("Remove Data Node");
		context = e.getDataNode();
	}

	public void currentDataMapChanged(DataMapDisplayEvent e){
		enableDataMapMenu();
		removeMenu.setText("Remove Data Map");
		context = e.getDataMap();
	}

   	public void currentObjEntityChanged(EntityDisplayEvent e)
   	{
		enableDataMapMenu();
		removeMenu.setText("Remove Obj Entity");
		context = e.getEntity();
   	}


   	public void currentDbEntityChanged(EntityDisplayEvent e)
   	{
		enableDataMapMenu();
		removeMenu.setText("Remove Db Entity");
		context = e.getEntity();
   	}


    /** Disables all menu  for the case when no project is open.
      * The only menu-s never disabled are "New Project", "Open Project" 
      * and "Exit". */
    private void disableMenu() {
        createDomainMenu.setEnabled(false);
        createDataMapMenu.setEnabled(false);
        createDataSourceMenu.setEnabled(false);
        createObjEntityMenu.setEnabled(false);
        createDbEntityMenu.setEnabled(false);
        openDataMapMenu.setEnabled(false);
        closeProjectMenu.setEnabled(false);
        saveProjectMenu.setEnabled(false);
        saveMapMenu.setEnabled(false);
        saveMapAsMenu.setEnabled(false);
        saveAllMenu.setEnabled(false);

        importDbMenu.setEnabled(false);
        generateMenu.setEnabled(false);
    }

	private void enableDomainMenu() {
		disableMenu();
		createDataMapMenu.setEnabled(true);
        createDomainMenu.setEnabled(true);
		createDataSourceMenu.setEnabled(true);
		closeProjectMenu.setEnabled(true);
        openDataMapMenu.setEnabled(true);
	    saveProjectMenu.setEnabled(true);
	    saveAllMenu.setEnabled(true);
        importDbMenu.setEnabled(true);
	}
	
	private void enableDataMapMenu() {
		saveMapAsMenu.setEnabled(true);		
		enableDomainMenu();
	    saveMapMenu.setEnabled(true);
        saveMapAsMenu.setEnabled(true);
        createObjEntityMenu.setEnabled(true);
        createDbEntityMenu.setEnabled(true);
        generateMenu.setEnabled(true);
	}
	
    public static void main(String[] args) 
    {
    	JFrame frame = getFrame();
    	//Center the window
    	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    	Dimension frameSize = frame.getSize();
    	if (frameSize.height > screenSize.height) {
      		frameSize.height = screenSize.height;
    	}
    	if (frameSize.width > screenSize.width) {
      		frameSize.width = screenSize.width;
    	}
    	frame.setLocation((screenSize.width - frameSize.width) / 2
    					 ,(screenSize.height - frameSize.height) / 2);
   		frame.setVisible(true);
   	}
}