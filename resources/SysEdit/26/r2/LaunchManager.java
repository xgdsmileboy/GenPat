/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids - bug 50567 Eclipse native environment support on Win98
 *     Pawel Piech - Bug 82001: When shutting down the IDE, the debugger should first
 *     attempt to disconnect debug targets before terminating them
 *******************************************************************************/
package org.eclipse.debug.internal.core;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.debug.internal.core.sourcelookup.SourceContainerType;
import org.eclipse.debug.internal.core.sourcelookup.SourcePathComputer;
import org.eclipse.osgi.service.environment.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.icu.text.MessageFormat;

/**
 * Manages launch configurations, launch configuration types, and registered launches.
 *
 * @see ILaunchManager
 */
/**
 * LaunchManager
 */
public class LaunchManager extends PlatformObject implements ILaunchManager, IResourceChangeListener {
	
	
	/**
	 * Constants for xml node names
	 * 
	 * @since 3.3
	 * 
	 * EXPERIMENTAL
	 */
	protected static final String MODES = "modes"; //$NON-NLS-1$
	protected static final String TYPEID = "typeid"; //$NON-NLS-1$
	protected static final String ID = "id"; //$NON-NLS-1$
	protected static final String DELEGATE = "delegate"; //$NON-NLS-1$
	protected static final String PREFERRED_DELEGATES = "preferredDelegates"; //$NON-NLS-1$
	protected static final String PREF_PREFERRED_DELEGATES = DebugPlugin.getUniqueIdentifier() + ".PREFERRED_DELEGATES"; //$NON-NLS-1$

	/**
	 * Constant to define debug.core for the status codes
	 * 
	 * @since 3.2
	 */
	private static final String DEBUG_CORE = "org.eclipse.debug.core"; //$NON-NLS-1$
	
	/**
	 * Constant to define debug.ui for the status codes
	 * 
	 * @since 3.2
	 */
	private static final String DEBUG_UI = "org.eclipse.debug.ui"; //$NON-NLS-1$
	
	/**
	 * Constant to represent the empty string
	 * 
	 * @since 3.2
	 */
	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$
    
	/**
	 * Status code for which a UI prompter is registered.
	 * 
	 * @since 3.2
	 */
	protected static final IStatus promptStatus = new Status(IStatus.INFO, DEBUG_UI, 200, EMPTY_STRING, null);
	
    /**
	 * Status code for which a prompter will ask the user to delete any/all of the launch configurations 
	 * that are associated with this project being deleted
	 * 
	 * @since 3.2
	 */
	protected static final IStatus deleteAssociatedLaunchConfigs = new Status(IStatus.INFO, DEBUG_CORE, 225, EMPTY_STRING, null);
	
	
	/**
	 * Notifies a launch config listener in a safe runnable to handle
	 * exceptions.
	 */
	class ConfigurationNotifier implements ISafeRunnable {
		
		private ILaunchConfigurationListener fListener;
		private int fType;
		private ILaunchConfiguration fConfiguration;
		
		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during launch configuration change notification.", exception);  //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		/**
		 * Notifies the given listener of the add/change/remove
		 * 
		 * @param configuration the configuration that has changed
		 * @param update the type of change
		 */
		public void notify(ILaunchConfiguration configuration, int update) {
			fConfiguration = configuration;
			fType = update;
			if (fLaunchConfigurationListeners.size() > 0) {
				Object[] listeners = fLaunchConfigurationListeners.getListeners();
				for (int i = 0; i < listeners.length; i++) {
					fListener = (ILaunchConfigurationListener)listeners[i];
                    SafeRunner.run(this);
				}
			}
			fConfiguration = null;
			fListener = null;			
		}

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.launchConfigurationAdded(fConfiguration);
					break;
				case REMOVED:
					fListener.launchConfigurationRemoved(fConfiguration);
					break;
				case CHANGED:
					fListener.launchConfigurationChanged(fConfiguration);
					break;
			}			
		}
	}
	
	/**
	 * Notifies a launch listener (multiple launches) in a safe runnable to
	 * handle exceptions.
	 */
	class LaunchesNotifier implements ISafeRunnable {
		
		private ILaunchesListener fListener;
		private int fType;
		private ILaunch[] fNotifierLaunches;
		private ILaunch[] fRegistered;
		
		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during launch change notification.", exception);  //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		/**
		 * Notifies the given listener of the adds/changes/removes
		 * 
		 * @param launches the launches that changed
		 * @param update the type of change
		 */
		public void notify(ILaunch[] launches, int update) {
			fNotifierLaunches = launches;
			fType = update;
			fRegistered = null;
			Object[] copiedListeners= fLaunchesListeners.getListeners();
			for (int i= 0; i < copiedListeners.length; i++) {
				fListener = (ILaunchesListener)copiedListeners[i];
                SafeRunner.run(this);
			}	
			fNotifierLaunches = null;
			fRegistered = null;
			fListener = null;			
		}

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.launchesAdded(fNotifierLaunches);
					break;
				case REMOVED:
					fListener.launchesRemoved(fNotifierLaunches);
					break;
				case CHANGED:
				case TERMINATE:
					if (fRegistered == null) {
						List registered = null;
						for (int j = 0; j < fNotifierLaunches.length; j++) {
							if (isRegistered(fNotifierLaunches[j])) {
								if (registered != null) {
									registered.add(fNotifierLaunches[j]);
								} 
							} else {
								if (registered == null) {
									registered = new ArrayList(fNotifierLaunches.length);
									for (int k = 0; k < j; k++) {
										registered.add(fNotifierLaunches[k]);
									}
								}
							}
						}
						if (registered == null) {
							fRegistered = fNotifierLaunches;
						} else {
							fRegistered = (ILaunch[])registered.toArray(new ILaunch[registered.size()]);
						}
					}
					if (fRegistered.length > 0) {
						if (fType == CHANGED) {
							fListener.launchesChanged(fRegistered);
						}
						if (fType == TERMINATE && fListener instanceof ILaunchesListener2) {
							((ILaunchesListener2)fListener).launchesTerminated(fRegistered);
						}
					}
					break;
			}
		}
	}
	
	/**
	 * Visitor for handling resource deltas.
	 */
	class LaunchManagerVisitor implements IResourceDeltaVisitor {
	    
	    /**
	     * Map of files to associated (shared) launch configs in a project
	     * that is going to be deleted.
	     */
	    private Map fFileToConfig = new HashMap();
	    
	    
		/**
         * Builds a cache of configs that will be deleted in the given project
         */
        public void preDelete(IProject project) {
            List list = findLaunchConfigurations(project);
            Iterator configs = list.iterator();
            while (configs.hasNext()) {
                ILaunchConfiguration configuration = (ILaunchConfiguration) configs.next();
                IFile file = configuration.getFile();
                if (file != null) {
                    fFileToConfig.put(file, configuration);
                }
            }
        }	
		
		/**
		 * Resets this resource delta visitor for a new pass.
		 */
		public void reset() {
		      fFileToConfig.clear();
		}

        /**
		 * @see IResourceDeltaVisitor#visit(IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) {
			if (delta == null) {
				return false;
			}
			if (0 != (delta.getFlags() & IResourceDelta.OPEN)) {
				if (delta.getResource() instanceof IProject) {
					IProject project = (IProject)delta.getResource();
					
					if (project.isOpen()) {
						LaunchManager.this.projectOpened(project);
					} else { 
					    LaunchManager.this.projectClosed(project);
					}
				}
				return false;
			}
			IResource resource = delta.getResource();
			if (resource instanceof IFile) {
				IFile file = (IFile)resource;
				if (ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION.equals(file.getFileExtension())) {
					IPath configPath = file.getLocation();
					ILaunchConfiguration handle = null;
					// If the file has already been deleted, reconstruct the handle from our cache
					if (configPath == null) {
					    handle = (ILaunchConfiguration) fFileToConfig.get(file);
					} else {
					    handle = new LaunchConfiguration(configPath);
					}
					if (handle != null) {
						switch (delta.getKind()) {						
							case IResourceDelta.ADDED :
								LaunchManager.this.launchConfigurationAdded(handle);
								break;
							case IResourceDelta.REMOVED :
								LaunchManager.this.launchConfigurationDeleted(handle);
								break;
							case IResourceDelta.CHANGED :
								LaunchManager.this.launchConfigurationChanged(handle);
								break;
						}
					}
				}
				return false;
			} else if (resource instanceof IContainer) {
				return true;
			}
			return true;
		}
	}
	
	/**
	 * Notifies a launch listener (single launch) in a safe runnable to handle
	 * exceptions.
	 */
	class LaunchNotifier implements ISafeRunnable {
		
		private ILaunchListener fListener;
		private int fType;
		private ILaunch fLaunch;
		
		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during launch change notification.", exception);  //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		/**
		 * Notifies the given listener of the add/change/remove
		 * 
		 * @param listener the listener to notify
		 * @param launch the launch that has changed
		 * @param update the type of change
		 */
		public void notify(ILaunch launch, int update) {
			fLaunch = launch;
			fType = update;
			Object[] copiedListeners= fListeners.getListeners();
			for (int i= 0; i < copiedListeners.length; i++) {
				fListener = (ILaunchListener)copiedListeners[i];
                SafeRunner.run(this);
			}	
			fLaunch = null;
			fListener = null;		
		}

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.launchAdded(fLaunch);
					break;
				case REMOVED:
					fListener.launchRemoved(fLaunch);
					break;
				case CHANGED:
					if (isRegistered(fLaunch)) {
						fListener.launchChanged(fLaunch);
					}
					break;
			}			
		}
	}
	
	/**
	 * Collects files whose extension matches the launch configuration file
	 * extension.
	 */
	class ResourceProxyVisitor implements IResourceProxyVisitor {
		
		private List fList;
		
		protected ResourceProxyVisitor(List list) {
			fList= list;
		}
		/**
		 * @see org.eclipse.core.resources.IResourceProxyVisitor#visit(org.eclipse.core.resources.IResourceProxy)
		 */
		public boolean visit(IResourceProxy proxy) {
			if (proxy.getType() == IResource.FILE) {
				if (ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION.equalsIgnoreCase(proxy.requestFullPath().getFileExtension())) {
					fList.add(proxy.requestResource());
				}
				return false;
			}
			return true;
		}
	}
	
	/**
	 * Internal class used to hold information about a preferred delegate
	 * @since 3.3
	 * 
	 * EXPERIMENTAL
	 */
	class PreferredDelegate {
		private ILaunchDelegate fDelegate = null;
		private String fTypeid = null;
		private Set fModes = null;
		
		public PreferredDelegate(ILaunchDelegate delegate, String typeid, Set modes) {
			fDelegate = delegate;
			fTypeid = typeid;
			fModes = modes;
		}
		
		public String getTypeId() {
			return fTypeid;
		}
		
		public Set getModes() {
			return fModes;
		}
		
		public ILaunchDelegate getDelegate() {
			return fDelegate;
		}
	}
	
	/**
	 * Types of notifications
	 */
	public static final int ADDED = 0;
	public static final int REMOVED= 1;
	public static final int CHANGED= 2;
	public static final int TERMINATE= 3;
	
	/**
	 * The collection of native environment variables on the user's system. Cached
	 * after being computed once as the environment cannot change.
	 */
	private static HashMap fgNativeEnv= null;
	private static HashMap fgNativeEnvCasePreserved= null;
	
	/**
	 * Path to the local directory where local launch configurations
	 * are stored with the workspace.
	 */
	protected static final IPath LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH =
		DebugPlugin.getDefault().getStateLocation().append(".launches"); //$NON-NLS-1$
	/**
	 * Returns a Document that can be used to build a DOM tree
	 * @return the Document
	 * @throws ParserConfigurationException if an exception occurs creating the document builder
	 * @since 3.0
	 */
	public static Document getDocument() throws ParserConfigurationException {
		DocumentBuilderFactory dfactory= DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder= dfactory.newDocumentBuilder();
		Document doc= docBuilder.newDocument();
		return doc;
	}

	/**
	 * Serializes a XML document into a string - encoded in UTF8 format,
	 * with platform line separators.
	 * 
	 * @param doc document to serialize
	 * @return the document as a string
	 * @throws TransformerException if an unrecoverable error occurs during the serialization
	 * @throws IOException if the encoding attempted to be used is not supported
	 */
	public static String serializeDocument(Document doc) throws TransformerException, IOException {
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		DOMSource source = new DOMSource(doc);
		StreamResult outputTarget = new StreamResult(s);
		transformer.transform(source, outputTarget);
		return s.toString("UTF8"); //$NON-NLS-1$			
	}
	
	/**
	 * Collection of defined launch configuration type
	 * extensions.
	 */
	private List fLaunchConfigurationTypes = null; 

	/**
	 * Launch configuration cache. Keys are <code>LaunchConfiguration</code>,
	 * values are <code>LaunchConfigurationInfo</code>.
	 */
	private Map fLaunchConfigurations = new HashMap(10);
	
	/**
	 * A cache of launch configuration names currently in the workspace.
	 */
	private String[] fSortedConfigNames = null;
	
	/**
	 * Collection of all launch configurations in the workspace.
	 * <code>List</code> of <code>ILaunchConfiguration</code>.
	 */
	private List fLaunchConfigurationIndex = null;
	
	/**
	 * Launch configuration comparator extensions,
	 * keyed by attribute name.
	 */
	private Map fComparators = null;
	
	/**
	 * Registered launch modes, or <code>null</code> if not initialized.
	 * Keys are mode identifiers, values are <code>ILaunchMode</code>s.
	 */
	private Map fLaunchModes = null;
		
	/**
	 * A map of LaunchDelegate objects stored by id of delegate, or launch config type
	 */
	private HashMap fLaunchDelegates = null;
	
	/**
	 * Initial startup cache of preferred delegate so that the debug prefs are only parsed once
	 * 
	 * @since 3.3
	 * 
	 * EXPERIMENTAL
	 */
	private Set fPreferredDelegates = null;
	
	/**
	 * Collection of launches
	 */
	private List fLaunches= new ArrayList(10);
	/**
	 * Set of launches for efficient 'isRegistered()' check
	 */
	private Set fLaunchSet = new HashSet(10);
	
	/**
	 * Collection of listeners
	 */
	private ListenerList fListeners = new ListenerList();
	
	/**
	 * Collection of "plural" listeners.
	 * @since 2.1
	 */
	private ListenerList fLaunchesListeners = new ListenerList();	
	
	/**
	 * Visitor used to process resource deltas,
	 * to update launch configuration index.
	 */
	private LaunchManagerVisitor fgVisitor;
	
	/**
	 * Whether this manager is listening for resource change events
	 */
	private boolean fListening = false;
	
	/**
	 * Launch configuration listeners
	 */
	private ListenerList fLaunchConfigurationListeners = new ListenerList();
			
	/**
	 * Table of source locator extensions. Keys
	 * are identifiers, and values are associated
	 * configuration elements.
	 */
	private Map fSourceLocators = null;

	/**
	 * The handles of launch configurations being moved, or <code>null</code>
	 */
	private ILaunchConfiguration fFrom;
	
	private ILaunchConfiguration fTo;

    /**
	 * Map of source container type extensions. Keys are extension ids
	 * and values are associated configuration elements.
	 */
	private Map sourceContainerTypes;
	
	/**
	 * Map of source path computer extensions. Keys are extension ids
	 * and values are associated configuration elements.
	 */
	private Map sourcePathComputers;

	/**
	 * @see ILaunchManager#addLaunch(ILaunch)
	 */
	public void addLaunch(ILaunch launch) {
		if (internalAddLaunch(launch)) {
			fireUpdate(launch, ADDED);
			fireUpdate(new ILaunch[] {launch}, ADDED);
		}
	}
		
	/**
	 * @see ILaunchManager#addLaunchConfigurationListener(ILaunchConfigurationListener)
	 */
	public void addLaunchConfigurationListener(ILaunchConfigurationListener listener) {
		fLaunchConfigurationListeners.add(listener);
	}	
	
	/**
	 * @see org.eclipse.debug.core.ILaunchManager#addLaunches(org.eclipse.debug.core.ILaunch)
	 */
	public void addLaunches(ILaunch[] launches) {
		List added = new ArrayList(launches.length);
		for (int i = 0; i < launches.length; i++) {
			if (internalAddLaunch(launches[i])) {
				added.add(launches[i]);
			}
		}
		if (!added.isEmpty()) {
			ILaunch[] addedLaunches = (ILaunch[])added.toArray(new ILaunch[added.size()]);
			fireUpdate(addedLaunches, ADDED);
			for (int i = 0; i < addedLaunches.length; i++) {
				fireUpdate(launches[i], ADDED);
			}
		}
	}
	
	/**
	 * @see org.eclipse.debug.core.ILaunchManager#addLaunchListener(org.eclipse.debug.core.ILaunchesListener)
	 */
	public void addLaunchListener(ILaunchesListener listener) {
		fLaunchesListeners.add(listener);
	}	
	
	/**
	 * @see ILaunchManager#addLaunchListener(ILaunchListener)
	 */
	public void addLaunchListener(ILaunchListener listener) {
		fListeners.add(listener);
	}	
	
	/**
	 * Computes and caches the native system environment variables as a map of
	 * variable names and values (Strings) in the given map.
	 * <p>
	 * Note that WIN32 system environment preserves
	 * the case of variable names but is otherwise case insensitive.
	 * Depending on what you intend to do with the environment, the
	 * lack of normalization may or may not be create problems. This
	 * method preserves mixed-case keys using the variable names 
	 * recorded by the OS.
	 * </p>
	 * @since 3.1
	 */	
	private void cacheNativeEnvironment(Map cache) {
		try {
			String nativeCommand= null;
			boolean isWin9xME= false; //see bug 50567
			String fileName= null;
			if (Platform.getOS().equals(Constants.OS_WIN32)) {
				String osName= System.getProperty("os.name"); //$NON-NLS-1$
				isWin9xME= osName != null && (osName.startsWith("Windows 9") || osName.startsWith("Windows ME")); //$NON-NLS-1$ //$NON-NLS-2$
				if (isWin9xME) {
					// Win 95, 98, and ME
					// SET might not return therefore we pipe into a file
					IPath stateLocation= DebugPlugin.getDefault().getStateLocation();
					fileName= stateLocation.toOSString() + File.separator  + "env.txt"; //$NON-NLS-1$
					nativeCommand= "command.com /C set > " + fileName; //$NON-NLS-1$
				} else {
					// Win NT, 2K, XP
					nativeCommand= "cmd.exe /C set"; //$NON-NLS-1$
				}
			} else if (!Platform.getOS().equals(Constants.OS_UNKNOWN)){
				nativeCommand= "env";		 //$NON-NLS-1$
			}
			if (nativeCommand == null) {
				return;
			}
			Process process= Runtime.getRuntime().exec(nativeCommand);
			if (isWin9xME) {
				//read piped data on Win 95, 98, and ME
				Properties p= new Properties();
				File file= new File(fileName);
				FileInputStream stream= new FileInputStream(file);
				p.load(stream);
				stream.close();
				if (!file.delete()) {
					file.deleteOnExit(); // if delete() fails try again on VM close
				}
				for (Enumeration enumeration = p.keys(); enumeration.hasMoreElements();) {
					// Win32's environment vars are case insensitive. Put everything
					// to uppercase so that (for example) the "PATH" variable will match
					// "pAtH" correctly on Windows.
					String key= (String) enumeration.nextElement();
					//no need to cast value
					cache.put(key, p.get(key));
				}
			} else {
				//read process directly on other platforms
				//we need to parse out matching '{' and '}' for function declarations in .bash environments
				// pattern is [func name]=() { and we must find the '}' on its own line with no trailing ';'
				InputStream stream = process.getInputStream();
				InputStreamReader isreader = new InputStreamReader(stream);
				BufferedReader reader = new BufferedReader(isreader);
				String line = reader.readLine();
				String key = null;
				String value = null;
				while (line != null) {
					int func = line.indexOf("=()"); //$NON-NLS-1$
					if(func > 0) {
						key = line.substring(0, func);
						//scan until we find the closing '}' with no following chars
						value = line.substring(func+1);
						while(line != null && !line.equals("}")) { //$NON-NLS-1$
							line = reader.readLine();
							if(line != null) {
								value += line;
							}
						}
						line = reader.readLine();
					}
					else {
						int separator = line.indexOf('=');
						if (separator > 0) {
							key = line.substring(0, separator);
							value = line.substring(separator + 1);
							line = reader.readLine();
							if(line != null) {
								//this line has a '=' read ahead to check next line for '=', might be broken on more than one line
								separator = line.indexOf('=');
								while(separator < 0) {
									value += line.trim();
									line = reader.readLine();
									if(line == null) {
										//if next line read is the end of the file quit the loop
										break;
									}
									separator = line.indexOf('=');
								}
							}
						}
					}
					if(key != null) {
						cache.put(key, value);
						key = null;
						value = null;
					}
				}
				reader.close();
			}
		} catch (IOException e) {
			// Native environment-fetching code failed.
			// This can easily happen and is not useful to log.
		}
	}

	/**
	 * Clears all launch configurations (if any have been accessed)
	 */
	private void clearAllLaunchConfigurations() {
		if (fLaunchConfigurationTypes != null) {
			fLaunchConfigurationTypes.clear();
		}
		if (fLaunchConfigurationIndex != null) {
			fLaunchConfigurationIndex.clear();
		}
	}
			
	/**
	 * The launch config name cache is cleared when a config is added, deleted or changed.
	 */
	protected void clearConfigNameCache() {
		fSortedConfigNames = null;
	}

	/**
	 * Return an instance of DebugException containing the specified message and Throwable.
	 */
	protected DebugException createDebugException(String message, Throwable throwable) {
		return new DebugException(
					new Status(
					 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
					 DebugException.REQUEST_FAILED, message, throwable 
					)
				);
	}
	
	/**
	 * Return a LaunchConfigurationInfo object initialized from XML contained in
	 * the specified stream.  Simply pass out any exceptions encountered so that
	 * caller can deal with them.  This is important since caller may need access to the
	 * actual exception.
	 */
	protected LaunchConfigurationInfo createInfoFromXML(InputStream stream) throws CoreException,
																			 ParserConfigurationException,
																			 IOException,
																			 SAXException {
		Element root = null;
		DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		parser.setErrorHandler(new DefaultHandler());
		root = parser.parse(new InputSource(stream)).getDocumentElement();
		LaunchConfigurationInfo info = new LaunchConfigurationInfo();
		info.initializeFromXML(root);
		return info;
	}	
	
	/**
	 * Finds and returns all launch configurations in the given
	 * container (and subcontainers)
	 * 
	 * @param container the container to search
	 * @exception CoreException an exception occurs traversing
	 *  the container.
	 * @return all launch configurations in the given container
	 */
	protected List findLaunchConfigurations(IContainer container) {
		List list = new ArrayList(10);
		if (container instanceof IProject && !((IProject)container).isOpen()) {
			return list;
		}
		ResourceProxyVisitor visitor= new ResourceProxyVisitor(list);
		try {
			container.accept(visitor, IResource.NONE);
		} catch (CoreException ce) {
			//Closed project...should not be possible with previous check
		}
		Iterator iter = list.iterator();
		List configs = new ArrayList(list.size());
		IFile file = null;
		while (iter.hasNext()) {
			file = (IFile)iter.next();
			configs.add(getLaunchConfiguration(file));
		}
		return configs;
	}
	
	/**
	 * Finds and returns all local launch configurations.
	 *
	 * @return all local launch configurations
	 * @exception CoreException if there is a lower level
	 *  IO exception
	 */
	protected List findLocalLaunchConfigurations() {
		IPath containerPath = LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH;
		List configs = new ArrayList(10);
		final File directory = containerPath.toFile();
		if (directory.isDirectory()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return dir.equals(directory) &&
							name.endsWith(ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION);
				}
			};
			String[] files = directory.list(filter);
			LaunchConfiguration config = null;
			for (int i = 0; i < files.length; i++) {
				config = new LaunchConfiguration(containerPath.append(files[i]));
				configs.add(config);
			}
		}
		return configs;
	}
		
	/**
	 * Fires notification to (single) listeners that a launch has been
	 * added/changed/removed..
	 */
	public void fireUpdate(ILaunch launch, int update) {
		new LaunchNotifier().notify(launch, update);
	}

	/**
	 * Fires notification to (multi) listeners that a launch has been
	 * added/changed/removed.
	 */
	public void fireUpdate(ILaunch[] launches, int update) {
		new LaunchesNotifier().notify(launches, update);
	}
							
	/**
	 * @see org.eclipse.debug.core.ILaunchManager#generateUniqueLaunchConfigurationNameFrom(String)
	 */
	public String generateUniqueLaunchConfigurationNameFrom(String baseName) {
		int index = 1;
		int length= baseName.length();
		int copyIndex = baseName.lastIndexOf(" ("); //$NON-NLS-1$
		if (copyIndex > -1 && length > copyIndex + 2 && baseName.charAt(length - 1) == ')') {
			String trailer = baseName.substring(copyIndex + 2, length -1);
			if (isNumber(trailer)) {
				try {
					index = Integer.parseInt(trailer);
					baseName = baseName.substring(0, copyIndex);
				} 
				catch (NumberFormatException nfe) {}
			}
		} 
		String newName = baseName;
		
		StringBuffer buffer = null;
		while (isExistingLaunchConfigurationName(newName)) {
			buffer = new StringBuffer(baseName);
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(String.valueOf(index));
			index++;
			buffer.append(')');
			newName = buffer.toString();		
		}		
		
		return newName;
	}
	
	/**
	 * Returns a collection of all launch configuration handles in 
	 * the workspace. This collection is initialized lazily.
	 * 
	 * @return all launch configuration handles
	 */
	private List getAllLaunchConfigurations() {
		if (fLaunchConfigurationIndex == null) {
			try {			
				fLaunchConfigurationIndex = new ArrayList(20);
				List configs = findLocalLaunchConfigurations();
				verifyConfigurations(configs, fLaunchConfigurationIndex);
				configs = findLaunchConfigurations(ResourcesPlugin.getWorkspace().getRoot());
				verifyConfigurations(configs, fLaunchConfigurationIndex);
			} finally {
				hookResourceChangeListener();				
			}
		}
		return fLaunchConfigurationIndex;
	}
	
	/**
	 * Return a sorted array of the names of all <code>ILaunchConfiguration</code>s in 
	 * the workspace.  These are cached, and cache is cleared when a new config is added,
	 * deleted or changed.
	 */
	protected String[] getAllSortedConfigNames() {
		if (fSortedConfigNames == null) {
			ILaunchConfiguration[] configs = getLaunchConfigurations();
			fSortedConfigNames = new String[configs.length];
			for (int i = 0; i < configs.length; i++) {
				fSortedConfigNames[i] = configs[i].getName();
			}
			Arrays.sort(fSortedConfigNames);
		}
		return fSortedConfigNames;
	}
	
	/**
	 * Returns the comparator registered for the given attribute, or
	 * <code>null</code> if none.
	 * 
	 * @param attributeName attribute for which a comparator is required
	 * @return comparator, or <code>null</code> if none
	 */
	protected Comparator getComparator(String attributeName) {
		 Map map = getComparators();
		 return (Comparator)map.get(attributeName);
	}
	
	/**
	 * Returns comparators, loading if required
	 */
	protected Map getComparators() {
		initializeComparators();
		return fComparators;
	}	
	
	/**
	 * Returns the launch configurations specified by the given
	 * XML document.
	 * 
	 * @param root XML document
	 * @return list of launch configurations
	 * @exception IOException if an exception occurs reading the XML
	 */	
	protected List getConfigsFromXML(Element root) throws CoreException {
		DebugException invalidFormat = 
			new DebugException(
				new Status(
				 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
				 DebugException.REQUEST_FAILED, DebugCoreMessages.LaunchManager_Invalid_launch_configuration_index__18, null 
				)
			);		
			
		if (!root.getNodeName().equalsIgnoreCase("launchConfigurations")) { //$NON-NLS-1$
			throw invalidFormat;
		}
		
		// read each launch configuration 
		List configs = new ArrayList(4);	
		NodeList list = root.getChildNodes();
		int length = list.getLength();
		Node node = null;
		Element entry = null;
		String memento = null;
		for (int i = 0; i < length; ++i) {
			node = list.item(i);
			short type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				entry = (Element) node;
				if (!entry.getNodeName().equals("launchConfiguration")) { //$NON-NLS-1$
					throw invalidFormat;
				}
				memento = entry.getAttribute("memento"); //$NON-NLS-1$
				if (memento == null) {
					throw invalidFormat;
				}
				configs.add(getLaunchConfiguration(memento));
			}
		}
		return configs;
	}
	
	protected ConfigurationNotifier getConfigurationNotifier() {
		return new ConfigurationNotifier();
	}			
	
	/**
	 * @see ILaunchManager#getDebugTargets()
	 */
	public IDebugTarget[] getDebugTargets() {
		synchronized (fLaunches) {
			List allTargets= new ArrayList(fLaunches.size());
			if (fLaunches.size() > 0) {
				Iterator e = fLaunches.iterator();
				IDebugTarget[] targets = null;
				while (e.hasNext()) {
					targets = ((ILaunch) e.next()).getDebugTargets();
					for (int i = 0; i < targets.length; i++) {
						allTargets.add(targets[i]);
					}
				}
			}
			return (IDebugTarget[])allTargets.toArray(new IDebugTarget[allTargets.size()]);
		}
	}
	
	/**
	 * Returns the resource delta visitor for the launch manager.
	 * 
	 * @return the resource delta visitor for the launch manager
	 */
	private LaunchManagerVisitor getDeltaVisitor() {
	    if (fgVisitor == null) {
			fgVisitor= new LaunchManagerVisitor();
		}
	    return fgVisitor;
	}
	
	/** 
	 * Returns an array of environment variables to be used when
	 * launching the given configuration or <code>null</code> if unspecified.
	 * 
	 * @param configuration launch configuration
	 * @throws CoreException if unable to access associated attribute or if
	 * unable to resolve a variable in an environment variable's value
	 */
	public String[] getEnvironment(ILaunchConfiguration configuration) throws CoreException {
		Map configEnv = configuration.getAttribute(ATTR_ENVIRONMENT_VARIABLES, (Map) null);
		if (configEnv == null) {
			return null;
		}
		Map env = new HashMap();
		// build base environment
		boolean append = configuration.getAttribute(ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
		if (append) {
			env.putAll(getNativeEnvironmentCasePreserved());
		}
		
		// Add variables from config
		Iterator iter= configEnv.entrySet().iterator();
		boolean win32= Platform.getOS().equals(Constants.OS_WIN32);
		Map.Entry entry = null;
		String key = null;
		String value = null;
		Object nativeValue = null;
		Iterator envIter = null;
		Map.Entry nativeEntry = null;
		String nativeKey = null;
		while (iter.hasNext()) {
			entry = (Map.Entry) iter.next();
			key = (String) entry.getKey();
            value = (String) entry.getValue();
            // translate any string substitution variables
            if (value != null) {
                value = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
            }
            boolean added= false;
			if (win32) {
                // First, check if the key is an exact match for an existing key.
				nativeValue = env.get(key);
                if (nativeValue != null) {
                    // If an exact match is found, just replace the value
                    env.put(key, value);
                } else {
                    // Win32 vars are case-insensitive. If an exact match isn't found, iterate to
                    // check for a case-insensitive match. We maintain the key's case (see bug 86725),
                    // but do a case-insensitive comparison (for example, "pAtH" will still override "PATH").
                    envIter = env.entrySet().iterator();
                    while (envIter.hasNext()) {
                        nativeEntry = (Map.Entry) envIter.next();
                        nativeKey = (String) (nativeEntry).getKey();
                        if (nativeKey.equalsIgnoreCase(key)) {
                            nativeEntry.setValue(value);
                            added = true;
                            break;
                        }
                    }
                }
			}
            if (!added) {
                env.put(key, value);
            }
		}		
		
		iter = env.entrySet().iterator();
		List strings = new ArrayList(env.size());
		StringBuffer buffer = null;
		while (iter.hasNext()) {
			entry = (Map.Entry) iter.next();
			buffer = new StringBuffer((String) entry.getKey());
			buffer.append('=').append((String) entry.getValue());
			strings.add(buffer.toString());
		}
		return (String[]) strings.toArray(new String[strings.size()]);
	}
	
	/**
	 * Returns the info object for the specified launch configuration.
	 * If the configuration exists, but is not yet in the cache,
	 * an info object is built and added to the cache.
	 * 
	 * @exception CoreException if an exception occurs building
	 *  the info object
	 * @exception DebugException if the config does not exist
	 */
	protected LaunchConfigurationInfo getInfo(ILaunchConfiguration config) throws CoreException {
		LaunchConfigurationInfo info = (LaunchConfigurationInfo)fLaunchConfigurations.get(config);
		if (info == null) {
			if (config.exists()) {
				InputStream stream = null;
				try {
					if (config.isLocal()) {
						IPath path = config.getLocation();
						File file = path.toFile();				
						stream = new FileInputStream(file);
					} else {
						IFile file = ((LaunchConfiguration) config).getFile();
						if (file == null) {
							throw createDebugException(MessageFormat.format(DebugCoreMessages.LaunchManager_30, new String[] {config.getName()}), null); 
						}
						stream = file.getContents();
					}
					info = createInfoFromXML(stream);
					fLaunchConfigurations.put(config, info);
				} catch (FileNotFoundException e) {
					throwException(config, e);					
				} catch (SAXException e) {
					throwException(config, e);					
				} catch (ParserConfigurationException e) {
					throwException(config, e);					
				} catch (IOException e) {
					throwException(config, e);					
				} finally {
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException e) {
							throwException(config, e);					
						}
					}
				}
		
			} else {
				throw createDebugException(
					MessageFormat.format(DebugCoreMessages.LaunchManager_does_not_exist, new String[]{config.getName(), config.getLocation().toOSString()}), null); 
			}
		}
		return info;
	}
	
	/**
	 * @see ILaunchManager#getLaunchConfiguration(IFile)
	 */
	public ILaunchConfiguration getLaunchConfiguration(IFile file) {
		hookResourceChangeListener();				
		return new LaunchConfiguration(file.getLocation());
	}
	
	/**
	 * @see ILaunchManager#getLaunchConfiguration(String)
	 */
	public ILaunchConfiguration getLaunchConfiguration(String memento) throws CoreException {
		hookResourceChangeListener();
		return new LaunchConfiguration(memento);
	}
	
	/**
	 * @see ILaunchManager#getLaunchConfigurations()
	 */
	public ILaunchConfiguration[] getLaunchConfigurations() {
		List allConfigs = getAllLaunchConfigurations();
		return (ILaunchConfiguration[])allConfigs.toArray(new ILaunchConfiguration[allConfigs.size()]);
	}	
	
	/**
	 * @see ILaunchManager#getLaunchConfigurations(ILaunchConfigurationType)
	 */
	public ILaunchConfiguration[] getLaunchConfigurations(ILaunchConfigurationType type) throws CoreException {
		Iterator iter = getAllLaunchConfigurations().iterator();
		List configs = new ArrayList();
		ILaunchConfiguration config = null;
		while (iter.hasNext()) {
			config = (ILaunchConfiguration)iter.next();
			if (config.getType().equals(type)) {
				configs.add(config);
			}
		}
		return (ILaunchConfiguration[])configs.toArray(new ILaunchConfiguration[configs.size()]);
	}
	
	/**
	 * Returns all launch configurations that are stored as resources
	 * in the given project.
	 * 
	 * @param project a project
	 * @return collection of launch configurations that are stored as resources
	 *  in the given project
	 */
	protected List getLaunchConfigurations(IProject project) {
		Iterator iter = getAllLaunchConfigurations().iterator();
		List configs = new ArrayList();
		ILaunchConfiguration config = null;
		IFile file = null;
		while (iter.hasNext()) {
			config = (ILaunchConfiguration)iter.next();
			file = config.getFile();
			if (file != null && file.getProject().equals(project)) {
				configs.add(config);
			}
		}
		return configs;
	}
	
	/**
	 * @see ILaunchManager#getLaunchConfigurationType(String)
	 */
	public ILaunchConfigurationType getLaunchConfigurationType(String id) {
		ILaunchConfigurationType[] types = getLaunchConfigurationTypes();
		for(int i = 0; i < types.length; i++) {
			if (types[i].getIdentifier().equals(id)) {
				return types[i];
			}
		}
		return null;
	}

	/**
	 * @see ILaunchManager#getLaunchConfigurationTypes()
	 */
	public ILaunchConfigurationType[] getLaunchConfigurationTypes() {
		initializeLaunchConfigurationTypes();
		return (ILaunchConfigurationType[])fLaunchConfigurationTypes.toArray(new ILaunchConfigurationType[fLaunchConfigurationTypes.size()]);
	}
	
	/**
	 * @see ILaunchManager#getLaunches()
	 */
	public ILaunch[] getLaunches() {
		synchronized (fLaunches) {
			return (ILaunch[])fLaunches.toArray(new ILaunch[fLaunches.size()]);
		}
	}
	
	/**)
	 * @see org.eclipse.debug.core.ILaunchManager#getLaunchMode(java.lang.String)
	 */
	public ILaunchMode getLaunchMode(String mode) {
		initializeLaunchModes();
		return (ILaunchMode) fLaunchModes.get(mode);
	}
	
	/**
	 * @see org.eclipse.debug.core.ILaunchManager#getLaunchModes()
	 */
	public ILaunchMode[] getLaunchModes() {
		initializeLaunchModes();
		Collection collection = fLaunchModes.values();
		return (ILaunchMode[]) collection.toArray(new ILaunchMode[collection.size()]);
	}
	
	/**
	 * Returns all of the launch delegates. The rturned listing of delegates cannot be directly used to launch,
	 * instead the method <code>IlaunchDelegate.getDelegate</code> must be used to acquire an executable form of
	 * the delegate, allowing us to maintain lazy loading of the delegates themselves.
	 * @return all of the launch delegates
	 * @since 3.3
	 * <p>
	 * <strong>EXPERIMENTAL</strong>. This method has been added as
	 * part of a work in progress. There is no guarantee that this API will
	 * remain unchanged during the 3.3 release cycle. Please do not use this API
	 * without consulting with the Platform/Debug team.
	 * </p>
	 */
	public LaunchDelegate[] getLaunchDelegates() {
		initializeLaunchDelegates();
		Collection col = fLaunchDelegates.values();
		return (LaunchDelegate[]) col.toArray(new LaunchDelegate[col.size()]);
	}
	
	/**
	 * Returns the listing of launch delegates that apply to the specified 
	 * <code>ILaunchConfigurationType</code> id
	 * @param typeid the id of the launch configuration type to get delegates for 
	 * @return An array of <code>LaunchDelegate</code>s that apply to the specified launch configuration
	 * type, or an empty array, never <code>null</code>
	 * 
	 * @since 3.3
	 * 
	 * EXPERIMENTAL
	 */
	public LaunchDelegate[] getLaunchDelegates(String typeid) {
		initializeLaunchDelegates();
		ArrayList list = new ArrayList();
		LaunchDelegate ld = null;
		for(Iterator iter = fLaunchDelegates.keySet().iterator(); iter.hasNext();) {
			ld = (LaunchDelegate) fLaunchDelegates.get(iter.next());
			if(ld.getLaunchConfigurationTypeId().equals(typeid)) {
				list.add(ld);
			}
		}
		return (LaunchDelegate[]) list.toArray(new LaunchDelegate[list.size()]);
	}
	
	/**
	 * Initializes the listing of delegates available to the launching framework
	 * 
	 * <p>
	 * <strong>EXPERIMENTAL</strong>. This method has been added as
	 * part of a work in progress. There is no guarantee that this API will
	 * remain unchanged during the 3.3 release cycle. Please do not use this API
	 * without consulting with the Platform/Debug team.
	 * </p>
	 * @since 3.3
	 */
	private synchronized void initializeLaunchDelegates() {
		if(fLaunchDelegates == null) {
			fLaunchDelegates = new HashMap();
			//get all launch delegate contributions
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_DELEGATES);
			IConfigurationElement[] infos = extensionPoint.getConfigurationElements();
			LaunchDelegate delegate = null;
			for(int i = 0; i < infos.length; i++) {
				delegate = new LaunchDelegate(infos[i]);
				fLaunchDelegates.put(delegate.getId(), delegate);
			}
			//get all delegates from launch configuration type contributions
			extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_CONFIGURATION_TYPES);
			infos = extensionPoint.getConfigurationElements();
			for(int i = 0; i < infos.length; i++) {
				//must check to see if delegate is provided in contribution
				if(infos[i].getAttribute(IConfigurationElementConstants.DELEGATE) != null) {
					delegate = new LaunchDelegate(infos[i]);
					fLaunchDelegates.put(delegate.getId(), delegate);
				}
			}
		}
	}
	
	/**
	 * This method is used to initialize a simple listing of all preferred delegates, which is then used by each
	 * <code>ILaunchConfigurationType</code> to find if they have preferred delegates. Once an <code>ILaunchConfigurationType</code>
	 * has used this listing to initialize its preferred delegates ti will maintain changes to its preferred delegate, which are 
	 * then written back to the pref sotre only when the launch manager shuts down.
	 * 
	 * <p>
	 * This cache is not synchronized with the runtime preferred delegates stored in launch configuration types.
	 * </p>
	 * 
	 * @since 3.3
	 * 
	 * EXPERIMENTAL
	 */
	private synchronized void initializePreferredDelegates() {
		if(fPreferredDelegates == null) {
			fPreferredDelegates = new HashSet();
			Preferences prefs = DebugPlugin.getDefault().getPluginPreferences();
			String preferred = prefs.getString(LaunchManager.PREF_PREFERRED_DELEGATES);
			if(!EMPTY_STRING.equals(preferred)) {
				try {
					Element root = DebugPlugin.parseDocument(preferred);
					NodeList nodes = root.getElementsByTagName(LaunchManager.DELEGATE);
					Element element = null;
					String typeid = null;
					Set modeset = null;
					List modesets = null;
					LaunchDelegate[] extensions = null;
					ILaunchDelegate delegate = null;
					for(int i = 0; i < nodes.getLength(); i++) {
						element = (Element) nodes.item(i);
						typeid = element.getAttribute(LaunchManager.TYPEID);
						extensions = getLaunchDelegates(typeid);
						for(int j = 0; j < extensions.length; j++) {
							if(element.getAttribute(LaunchManager.ID).equals(extensions[j].getId())) { 
								modesets = extensions[j].getModes();
								String[] modes = element.getAttribute(LaunchManager.MODES).split(","); //$NON-NLS-1$
								modeset = new HashSet(Arrays.asList(modes));
								if(modesets.contains(modeset)) {
									delegate = extensions[j]; 
									break;
								}
							}
						}
						//take tid, modeset, delegate and create entry
						if(delegate != null & !EMPTY_STRING.equals(typeid) & modeset != null) {
							fPreferredDelegates.add(new PreferredDelegate(delegate, typeid, modeset));
						}
						delegate = null;
					}
				}
				catch (CoreException e) {DebugPlugin.log(e);} 
			}
		}
	}
	
	/**
	 * Allows internal access to a preferred delegate for a given type and mode set
	 * @param typeid the id of the <code>ILaunchConfigurationType</code> to find a delegate for
	 * @param modes ther set of modes for the delegate
	 * @return the preferred delegate for the specified type id and mode set, or <code>null</code> if none
	 * 
	 * @since 3.3
	 * 
	 * EXPERIMENTAL
	 */
	protected ILaunchDelegate getPreferredDelegate(String typeid, Set modes) {
		initializePreferredDelegates();
		PreferredDelegate pd = null;
		for(Iterator iter = fPreferredDelegates.iterator(); iter.hasNext();) {
			pd = (PreferredDelegate) iter.next();
			if(pd.getModes().equals(modes) & pd.getTypeId().equals(typeid)) {
				return pd.getDelegate();
			}
		}
		return null;
	}
	
	/**
	 * Returns all launch configurations that are stored locally.
	 * 
	 * @return collection of launch configurations stored locally
	 */
	protected List getLocalLaunchConfigurations() {
		Iterator iter = getAllLaunchConfigurations().iterator();
		List configs = new ArrayList();
		ILaunchConfiguration config = null;
		while (iter.hasNext()) {
			config = (ILaunchConfiguration)iter.next();
			if (config.isLocal()) {
				configs.add(config);
			}
		}
		return configs;
	}
	
	/**
	 * Returns the launch configurations mapping to the specified resource
	 * @param resource the resource to collect mapped launch configurations for
	 * @return a list of launch configurations if found or an empty list, never null
	 * @since 3.2
	 */
	public ILaunchConfiguration[] getMappedConfigurations(IResource resource) {
		List configurations = new ArrayList();
		try {
			ILaunchConfiguration[] configs = getLaunchConfigurations();
			IResource[] resources = null;
			for(int i = 0; i < configs.length; i++) {
				resources = configs[i].getMappedResources();
				if(resources != null) {
					for(int j = 0; j < resources.length; j++) {
						if(resources[j].equals(resource)) {
							configurations.add(configs[i]);
						}
					}
				}
			}
		}
		catch(CoreException e) {DebugPlugin.log(e);}
		return (ILaunchConfiguration[])configurations.toArray(new ILaunchConfiguration[configurations.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#getMigrationCandidates()
	 */
	public ILaunchConfiguration[] getMigrationCandidates() throws CoreException {
		List configs = new ArrayList();
		ILaunchConfiguration[] candidates = getLaunchConfigurations();
		for(int i = 0; i < candidates.length; i++) {
			if(candidates[i].isMigrationCandidate()) {
				configs.add(candidates[i]);
			}
		}
		return (ILaunchConfiguration[])configs.toArray(new ILaunchConfiguration[configs.size()]);
	}
	
	/**
	 * @see org.eclipse.debug.core.ILaunchManager#getMovedFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public ILaunchConfiguration getMovedFrom(ILaunchConfiguration addedConfiguration) {
		if (addedConfiguration.equals(fTo)) {
			return fFrom;
		}
		return null;
	}
	
	/**
	 * @see org.eclipse.debug.core.ILaunchManager#getMovedTo(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public ILaunchConfiguration getMovedTo(ILaunchConfiguration removedConfiguration) {
		if (removedConfiguration.equals(fFrom)) {
			return fTo;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#getNativeEnvironment()
	 */
	public synchronized Map getNativeEnvironment() {
		if (fgNativeEnv == null) {
			Map casePreserved = getNativeEnvironmentCasePreserved();
			if (Platform.getOS().equals(Constants.OS_WIN32)) {
				fgNativeEnv= new HashMap();
				Iterator entries = casePreserved.entrySet().iterator();
				Map.Entry entry = null;
				String key = null;
				while (entries.hasNext()) {
					entry = (Entry) entries.next();
					key = ((String)entry.getKey()).toUpperCase();
					fgNativeEnv.put(key, entry.getValue());
				}
			} else {
				fgNativeEnv = new HashMap(casePreserved);
			}
		}
		return new HashMap(fgNativeEnv);
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#getNativeEnvironmentCasePreserved()
	 */
	public synchronized Map getNativeEnvironmentCasePreserved() {
		if (fgNativeEnvCasePreserved == null) {
			fgNativeEnvCasePreserved= new HashMap();
			cacheNativeEnvironment(fgNativeEnvCasePreserved);
		}
		return new HashMap(fgNativeEnvCasePreserved);
	}
	
	/**
	 * @see ILaunchManager#getProcesses()
	 */
	public IProcess[] getProcesses() {
		synchronized (fLaunches) {
			List allProcesses = new ArrayList(fLaunches.size());
			Iterator e = fLaunches.iterator();
			IProcess[] processes = null;
			while (e.hasNext()) {
				processes = ((ILaunch) e.next()).getProcesses();
				for (int i= 0; i < processes.length; i++) {
					allProcesses.add(processes[i]);
				}
			}
			return (IProcess[])allProcesses.toArray(new IProcess[allProcesses.size()]);
		}
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#getSourceContainerType(java.lang.String)
	 */
	public ISourceContainerType getSourceContainerType(String id) {
		initializeSourceContainerTypes();
		return (ISourceContainerType) sourceContainerTypes.get(id);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#getSourceContainerTypes()
	 */
	public ISourceContainerType[] getSourceContainerTypes() {
		initializeSourceContainerTypes();
		Collection containers = sourceContainerTypes.values();
		return (ISourceContainerType[]) containers.toArray(new ISourceContainerType[containers.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#newSourcePathComputer(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public ISourcePathComputer getSourcePathComputer(ILaunchConfiguration configuration) throws CoreException {
		String id = null;
		id = configuration.getAttribute(ISourcePathComputer.ATTR_SOURCE_PATH_COMPUTER_ID, (String)null);
		
		if (id == null) {
			//use default computer for configuration type, if any			
			return configuration.getType().getSourcePathComputer();							
		}
		return getSourcePathComputer(id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#getSourcePathComputer(java.lang.String)
	 */
	public ISourcePathComputer getSourcePathComputer(String id) {
		initializeSourceContainerTypes();
		return (ISourcePathComputer) sourcePathComputers.get(id);
	}

	/**
     * Starts listening for resource change events
     */
    private synchronized void hookResourceChangeListener() {
        if (!fListening) {
        	ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE);
            fListening = true;
        }
    }
	
	/**
	 * Load comparator extensions.
	 */
	private synchronized void initializeComparators() {
		if (fComparators == null) {
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_CONFIGURATION_COMPARATORS);
			IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
			fComparators = new HashMap(infos.length);
			IConfigurationElement configurationElement = null;
			String attr = null;
			for (int i= 0; i < infos.length; i++) {
				configurationElement = infos[i];
				attr = configurationElement.getAttribute("attribute"); //$NON-NLS-1$			
				if (attr != null) {
					fComparators.put(attr, new LaunchConfigurationComparator(configurationElement));
				} else {
					// invalid status handler
					IStatus s = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.INTERNAL_ERROR,
					MessageFormat.format("Invalid launch configuration comparator extension defined by plug-in {0} - attribute not specified.", new String[] {configurationElement.getContributor().getName()}), null);  //$NON-NLS-1$
					DebugPlugin.log(s);
				}
			}
		}
	}

	/**
	 * Initializes the listing of <code>LaunchConfigurationType</code>s.
	 */
	private synchronized void initializeLaunchConfigurationTypes() {
		if (fLaunchConfigurationTypes == null) {
			hookResourceChangeListener();
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_CONFIGURATION_TYPES);
			IConfigurationElement[] infos = extensionPoint.getConfigurationElements();
			fLaunchConfigurationTypes = new ArrayList(infos.length);
			for (int i= 0; i < infos.length; i++) {		
				fLaunchConfigurationTypes.add(new LaunchConfigurationType(infos[i]));
			}
		}
	}
	
	/**
	 * Load comparator extensions.
	 * 
	 * @exception CoreException if an exception occurs reading
	 *  the extensions
	 *  
	 */
	private synchronized void initializeLaunchModes() {
		if (fLaunchModes == null) {
			try {
				IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_LAUNCH_MODES);
				IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
				fLaunchModes = new HashMap();
				ILaunchMode mode = null;
				for (int i= 0; i < infos.length; i++) {
					mode = new LaunchMode(infos[i]);
					fLaunchModes.put(mode.getIdentifier(), mode);
				}
			} 
			catch (CoreException e) {DebugPlugin.log(e);}
		}
	}
	
	/**
	 * Initializes source container type and source path computer extensions.
	 */
	private synchronized void initializeSourceContainerTypes() {
		if (sourceContainerTypes == null) {
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_SOURCE_CONTAINER_TYPES);
			IConfigurationElement[] extensions = extensionPoint.getConfigurationElements();
			sourceContainerTypes = new HashMap();
			for (int i = 0; i < extensions.length; i++) {
				sourceContainerTypes.put(
						extensions[i].getAttribute(ID),
						new SourceContainerType(extensions[i]));
			}
			extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_SOURCE_PATH_COMPUTERS);
			extensions = extensionPoint.getConfigurationElements();
			sourcePathComputers = new HashMap();
			for (int i = 0; i < extensions.length; i++) {
				sourcePathComputers.put(
						extensions[i].getAttribute(ID),
						new SourcePathComputer(extensions[i]));
			}
		}
	}

	/**
	 * Register source locators.
	 * 
	 * @exception CoreException if an exception occurs reading
	 *  the extensions
	 */
	private synchronized void initializeSourceLocators() {
		if (fSourceLocators == null) {
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_SOURCE_LOCATORS);
			IConfigurationElement[] infos= extensionPoint.getConfigurationElements();
			fSourceLocators= new HashMap(infos.length);
			IConfigurationElement configurationElement = null;
			String id = null;
			for (int i= 0; i < infos.length; i++) {
				configurationElement = infos[i];
				id = configurationElement.getAttribute(ID);			
				if (id != null) {
					fSourceLocators.put(id,configurationElement);
				} else {
					// invalid status handler
					IStatus s = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.INTERNAL_ERROR,
					MessageFormat.format("Invalid source locator extension defined by plug-in \"{0}\": \"id\" not specified.", new String[] {configurationElement.getContributor().getName()} ), null);   //$NON-NLS-1$
					DebugPlugin.log(s);
				}
			}
		}
	}

	/**
	 * Adds the given launch object to the list of registered launches,
	 * and returns whether the launch was added.
	 * 
	 * @param launch launch to register
	 * @return whether the launch was added
	 */
	protected boolean internalAddLaunch(ILaunch launch) {
		synchronized (fLaunches) {
			if (fLaunches.contains(launch)) {
				return false;
			}
			fLaunches.add(launch);
			fLaunchSet.add(launch);
			return true;
		}
	}

	/**
	 * Removes the given launch object from the collection of registered
	 * launches. Returns whether the launch was removed.
	 * 
	 * @param launch the launch to remove
	 * @return whether the launch was removed
	 */
	protected boolean internalRemoveLaunch(ILaunch launch) {
		if (launch == null) {
			return false;
		}
		synchronized (fLaunches) {
			fLaunchSet.remove(launch);
			return fLaunches.remove(launch);
		}
	}
	/**
	 * @see ILaunchManager#isExistingLaunchConfigurationName(String)
	 */
	public boolean isExistingLaunchConfigurationName(String name) {
		String[] sortedConfigNames = getAllSortedConfigNames();
		int index = Arrays.binarySearch(sortedConfigNames, name);
		if (index < 0) {
			return false;
		} 
		return true;
	}
	
	/**
	 * Returns whether the given String is composed solely of digits
	 */
	private boolean isNumber(String string) {
		int numChars= string.length();
		if (numChars == 0) {
			return false;
		}
		for (int i= 0; i < numChars; i++) {
			if (!Character.isDigit(string.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchManager#isRegistered(org.eclipse.debug.core.ILaunch)
	 */
	public boolean isRegistered(ILaunch launch) {
		synchronized (fLaunches) {
			return fLaunchSet.contains(launch);
		}
	}

	/**
	 * Returns whether the given launch configuration passes a basic
	 * integrity test by retrieving its type.
	 * 
	 * @param config the configuration to verify
	 * @return whether the config meets basic integrity constraints
	 */
	protected boolean isValid(ILaunchConfiguration config) {
		try {
			config.getType();
		} catch (CoreException e) {
			if (e.getStatus().getCode() != DebugException.MISSING_LAUNCH_CONFIGURATION_TYPE) {
				// only log warnings due to something other than a missing
				// launch config type
				DebugPlugin.log(e);
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Notifies the launch manager that a launch configuration
	 * has been added. The configuration is added to the index of
	 * configurations by project, and listeners are notified.
	 * 
	 * @param config the launch configuration that was added
	 */
	protected void launchConfigurationAdded(ILaunchConfiguration config) {
		if (config.isWorkingCopy()) {
			return;
		}
		if (isValid(config)) {
			List allConfigs = getAllLaunchConfigurations();
			if (!allConfigs.contains(config)) {
				allConfigs.add(config);
				getConfigurationNotifier().notify(config, ADDED);
				clearConfigNameCache();
			}
		} else {
			launchConfigurationDeleted(config);
		}
	}
	
	/**
	 * Notifies the launch manager that a launch configuration
	 * has been changed. The configuration is removed from the
	 * cache of info objects such that the new attributes will
	 * be updated on the next access. Listeners are notified of
	 * the change.
	 * 
	 * @param config the launch configuration that was changed
	 */
	protected void launchConfigurationChanged(ILaunchConfiguration config) {
		fLaunchConfigurations.remove(config);
		clearConfigNameCache();
		if (isValid(config)) {
			// in case the config has been refreshed and it was removed from the
			// index due to 'out of synch with local file system' (see bug 36147),
			// add it back (will only add if required)
			launchConfigurationAdded(config);
			getConfigurationNotifier().notify(config, CHANGED);
		} else {
			launchConfigurationDeleted(config);
		}								
	}
	
	/**
	 * Notifies the launch manager that a launch configuration
	 * has been deleted. The configuration is removed from the
	 * cache of infos and from the index of configurations by
	 * project, and listeners are notified.
	 * 
	 * @param config the launch configuration that was deleted
	 */
	protected void launchConfigurationDeleted(ILaunchConfiguration config) {
		fLaunchConfigurations.remove(config);
		getAllLaunchConfigurations().remove(config);
		getConfigurationNotifier().notify(config, REMOVED);
		clearConfigNameCache();			
	}
	
	/**
	 * @see ILaunchManager#newSourceLocator(String)
	 */
	public IPersistableSourceLocator newSourceLocator(String identifier) throws CoreException {
		initializeSourceLocators();
		IConfigurationElement config = (IConfigurationElement)fSourceLocators.get(identifier);
		if (config == null) {
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.INTERNAL_ERROR,
				MessageFormat.format(DebugCoreMessages.LaunchManager_Source_locator_does_not_exist___0__13, new String[] {identifier} ), null)); 
		} 
		IPersistableSourceLocator sourceLocator = (IPersistableSourceLocator)config.createExecutableExtension("class"); //$NON-NLS-1$
		if (sourceLocator instanceof AbstractSourceLookupDirector) {
			((AbstractSourceLookupDirector)sourceLocator).setId(identifier);
		}
		return sourceLocator;
	}
	
	/**
	 * The specified project has just closed - remove its
	 * launch configurations from the cached index.
	 * 
	 * @param project the project that has been closed
	 * @exception CoreException if writing the index fails
	 */
	protected void projectClosed(IProject project) {
		List configs = getLaunchConfigurations(project);
		if (!configs.isEmpty()) {
			Iterator iterator = configs.iterator();
			while (iterator.hasNext()) {
				launchConfigurationDeleted((ILaunchConfiguration)iterator.next());
			}
		}
		//bug 12134
		terminateMappedConfigurations(project);
	}
	
	/**
	 * The specified project has just opened - add all launch
	 * configs in the project to the index of all configs.
	 * 
	 * @param project the project that has been opened
	 * @exception CoreException if reading the index fails
	 */
	protected void projectOpened(IProject project) {
		List configs = findLaunchConfigurations(project);
		if (!configs.isEmpty()) {
			Iterator iterator = configs.iterator();
			while (iterator.hasNext()) {
				launchConfigurationAdded((ILaunchConfiguration) iterator.next());
			}			
		}
	}
	
	/**
	 * @see ILaunchManager#removeLaunch(ILaunch)
	 */
	public void removeLaunch(final ILaunch launch) {
		if (internalRemoveLaunch(launch)) {
			fireUpdate(launch, REMOVED);
			fireUpdate(new ILaunch[] {launch}, REMOVED);
		}
	}
	
	/**
	 * @see ILaunchManager#removeLaunchConfigurationListener(ILaunchConfigurationListener)
	 */
	public void removeLaunchConfigurationListener(ILaunchConfigurationListener listener) {
		fLaunchConfigurationListeners.remove(listener);
	}
	
	/**
	 * @see org.eclipse.debug.core.ILaunchManager#removeLaunches(org.eclipse.debug.core.ILaunch)
	 */
	public void removeLaunches(ILaunch[] launches) {
		List removed = new ArrayList(launches.length);
		for (int i = 0; i < launches.length; i++) {
			if (internalRemoveLaunch(launches[i])) {
				removed.add(launches[i]);
			}
		}
		if (!removed.isEmpty()) {
			ILaunch[] removedLaunches = (ILaunch[])removed.toArray(new ILaunch[removed.size()]);
			fireUpdate(removedLaunches, REMOVED);
			for (int i = 0; i < removedLaunches.length; i++) {
				fireUpdate(removedLaunches[i], REMOVED);
			}
		}
	}	
	/**
	 * @see org.eclipse.debug.core.ILaunchManager#removeLaunchListener(org.eclipse.debug.core.ILaunchesListener)
	 */
	public void removeLaunchListener(ILaunchesListener listener) {
		fLaunchesListeners.remove(listener);
	}
	
	/**
	 * @see ILaunchManager#removeLaunchListener(ILaunchListener)
	 */
	public void removeLaunchListener(ILaunchListener listener) {
		fListeners.remove(listener);
	}
	
	/**
	 * Traverses the delta looking for added/removed/changed launch
	 * configuration files.
	 * 
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta == null) {
		    // pre-delete
		    LaunchManagerVisitor visitor = getDeltaVisitor();
		    IResource resource = event.getResource();
		    if (resource instanceof IProject) {
                IProject project = (IProject) resource;
                visitor.preDelete(project);
            }
		} else {
			try {
				IResourceDelta[] children = delta.getAffectedChildren(IResourceDelta.REMOVED);
				if(children.length > 0) {
					//collect child resources that are projects and have been deleted, but not moved
					ArrayList projs = new ArrayList();
					IResource res = null;
					for (int i = 0; i < children.length; i++) {
						if(children[i].getFlags() != IResourceDelta.MOVED_TO) {
							res = children[i].getResource();
							if(res != null && res instanceof IProject) {
								projs.add(res);
							}
						}
					}
					if(projs.size() > 0) {
						IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(promptStatus);
						handler.handleStatus(deleteAssociatedLaunchConfigs, projs.toArray(new IProject[projs.size()]));
						projs.clear();
					}
				}
			    LaunchManagerVisitor visitor = getDeltaVisitor();
				delta.accept(visitor);
				visitor.reset();
			}
			catch (CoreException e) {DebugPlugin.log(e);}	
		}
	}
	/**
	 * Indicates the given launch configuration is being moved from the given
	 * location to the new location.
	 * 
	 * @param from the location a launch configuration is being moved from, or
	 * <code>null</code>
	 * @param to the location a launch configuration is being moved to,
	 * or <code>null</code>
	 */
	protected void setMovedFromTo(ILaunchConfiguration from, ILaunchConfiguration to) {
		fFrom = from;
		fTo = to;
	}
	/**
	 * Terminates/Disconnects any active debug targets/processes.
	 * Clears launch configuration types.
	 */
	public void shutdown() {
		fListeners = new ListenerList();
        fLaunchesListeners = new ListenerList();
        fLaunchConfigurationListeners = new ListenerList();
		ILaunch[] launches = getLaunches();
		ILaunch launch = null;
		for (int i= 0; i < launches.length; i++) {
			launch = launches[i];
			try {
                if (launch instanceof IDisconnect) {
                    IDisconnect disconnect = (IDisconnect)launch;
                    if (disconnect.canDisconnect()) {
                        disconnect.disconnect();
                    }
                }
                if (launch.canTerminate()) {
                    launch.terminate();
                }
			} catch (DebugException e) {
				DebugPlugin.log(e);
			}
		}
		
		persistPreferredLaunchDelegates();
		clearAllLaunchConfigurations();

		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	/**
	 * Saves the listings of preferred launch delegates from all of the launch configuration types
	 * 
	 * @since 3.3
	 * 
	 * EXPERIMENTAL
	 */
	private void persistPreferredLaunchDelegates() {
		Preferences prefs = DebugPlugin.getDefault().getPluginPreferences();
		try {
			Document doc = DebugPlugin.newDocument();
			Element root = doc.createElement(PREFERRED_DELEGATES);
			doc.appendChild(root);
			ILaunchConfigurationType[] types = getLaunchConfigurationTypes();
			Map preferred = null;
			Element child = null;
			ILaunchDelegate delegate = null;
			Set modes = null;
			String modestr = EMPTY_STRING;
			for(int i = 0; i < types.length; i++) {
				preferred = ((LaunchConfigurationType)types[i]).getPreferredDelegates();
				if(preferred != null && preferred.size() > 0) {
					for(Iterator iter = preferred.keySet().iterator(); iter.hasNext();) {
						modes = (Set) iter.next();
						delegate = (ILaunchDelegate) preferred.get(modes);
						child = doc.createElement(DELEGATE);
						child.setAttribute(ID, delegate.getId());
						child.setAttribute(TYPEID, types[i].getIdentifier());
						for(Iterator iter2 = modes.iterator(); iter2.hasNext();) {
							modestr += iter2.next();
							if(iter2.hasNext()) {
								modestr += ","; //$NON-NLS-1$
							}
						}
						child.setAttribute(MODES, modestr);
						modestr = EMPTY_STRING;
						root.appendChild(child);
					}
				}
			}
			String pref = null;
			if(root.hasChildNodes()) {
				pref = serializeDocument(doc);
			}
			if(pref != null) {
				prefs.setValue(PREF_PREFERRED_DELEGATES, pref);
			}
			if(prefs.needsSaving()) {
				DebugPlugin.getDefault().savePluginPreferences();
			}
		} 
		catch (CoreException e) {DebugPlugin.log(e);}
		catch (IOException ioe) {DebugPlugin.log(ioe);}
		catch (TransformerException te) {DebugPlugin.log(te);}
	}
	
	/**
	 * finds and terminates any running launch configurations associated with the given resource
	 * @param resource the resource to search for launch configurations and hence launches for
	 * @since 3.2
	 */
	protected void terminateMappedConfigurations(IResource resource) {
		ILaunch[] launches = getLaunches();
		ILaunchConfiguration[] configs = getMappedConfigurations(resource);
		try {
			for(int i = 0; i < launches.length; i++) {
				for(int j = 0; j < configs.length; j++) {
					if(configs[j].equals(launches[i].getLaunchConfiguration()) & launches[i].canTerminate()) {
						launches[i].terminate();
					}
				}
			}
		}
		catch(CoreException e) {DebugPlugin.log(e);}
	}
	
	/**
	 * Throws a debug exception with the given throwable that occurred
	 * while processing the given configuration.
	 */
	private void throwException(ILaunchConfiguration config, Throwable e) throws DebugException {
		IPath path = config.getLocation();
		throw createDebugException(MessageFormat.format(DebugCoreMessages.LaunchManager__0__occurred_while_reading_launch_configuration_file__1___1, new String[]{e.toString(), path.toOSString()}), e); 
	}

	/**
	 * Verify basic integrity of launch configurations in the given list,
	 * adding valid configs to the collection of all launch configurations.
	 * Exceptions are logged for invalid configs.
	 * 
	 * @param verify the list of configs to verify
	 * @param valid the list to place valid configurations in
	 */
	protected void verifyConfigurations(List verify, List valid) {
		Iterator configs = verify.iterator();
		ILaunchConfiguration config = null;
		while (configs.hasNext()) {
			config = (ILaunchConfiguration)configs.next();
			if (isValid(config)) {
				valid.add(config);
			}
		}		
	}
	
	/**
	 * Returns the name of the given launch mode with accelerators removed,
	 * or <code>null</code> if none.
	 * 
	 * @param id
	 */
	public String getLaunchModeName(String id) {
		ILaunchMode launchMode = getLaunchMode(id);
		if (launchMode != null) {
			return removeAccelerators(launchMode.getLabel());
		}
		return null;
	}
	/**
	 * Returns the label with any accelerators removed.
	 * 
	 * @return label without accelerators
	 */
    public static String removeAccelerators(String label) {
        String title = label;
        if (title != null) {
            // strip out any '&' (accelerators)
            int index = title.indexOf('&');
            if (index == 0) {
                title = title.substring(1);
            } else if (index > 0) {
                //DBCS languages use "(&X)" format
                if (title.charAt(index - 1) == '(' && title.length() >= index + 3 && title.charAt(index + 2) == ')') {
                    String first = title.substring(0, index - 1);
                    String last = title.substring(index + 3);
                    title = first + last;
                } else if (index < (title.length() - 1)) {
                    String first = title.substring(0, index);
                    String last = title.substring(index + 1);
                    title = first + last;
                }
            }
        }
        return title;
    }	
}
