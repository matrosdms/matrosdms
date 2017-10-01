package net.schwehla.matrosdms.rcp.wizzard;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.core.services.nls.Translation;
import org.eclipse.e4.core.services.statusreporter.StatusReporter;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.service.prefs.BackingStoreException;

import net.schwehla.matrosdms.domain.admin.MatrosConnectionCredential;
import net.schwehla.matrosdms.i18n.MatrosMessage;
import net.schwehla.matrosdms.persistenceservice.IMatrosServiceService;
import net.schwehla.matrosdms.rcp.MyGlobalConstants;
import net.schwehla.matrosdms.rcp.parts.helper.MatrosPreferenceInbox;
import net.schwehla.matrosdms.rcp.wizzard.model.setup.Masterdata;

@Creatable
@Singleton
public class OpenExistingWorker implements IRunnableWithProgress {

	@Inject
	IMatrosServiceService service;
	
	@Inject Masterdata masterData;
	
	@Inject  StatusReporter statusReporter;
	
	@Inject
	@Preference(nodePath = MyGlobalConstants.Preferences.NODE_COM_MATROSDMS) 
	IEclipsePreferences preferences ;

	
	@Inject Logger logger;
	 
	// get UISynchronize injected as field
	@Inject UISynchronize sync;

	boolean error;

	 
	@Inject
	@Translation
	MatrosMessage messages;
		
    private static final String FORCE_CLEAR_PERSISTED_STATE = "model.forceClearPersistedState"; //$NON-NLS-1$
	  
	 
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

		
		try {
			preferences.put(FORCE_CLEAR_PERSISTED_STATE, Boolean.TRUE.toString());
			preferences.flush();
		} catch (BackingStoreException e1) {
			logger.error(e1);
		}
	    
		
		// https://github.com/alblue/com.packtpub.e4/blob/master/com.packtpub.e4.clock.ui/src/com/packtpub/e4/clock/ui/handlers/HelloHandler.java
	     SubMonitor subMonitor = SubMonitor.convert(monitor,3);
        
                 try {
                	 
                     // sleep a second
            		 subMonitor.subTask(messages.setupworker_saving_inbox);
            	 	 Thread.sleep(500);
            	 	  	 
            	     sync.syncExec(new Runnable() {
                         @Override
                         public void run() {
                             
                          	 String inboxPath = masterData.getInboxList().stream().map(MatrosPreferenceInbox::getPath)
                          			 .collect(Collectors.joining(MyGlobalConstants.Preferences.DELIMITER));
                          	 
                          	preferences.put(MyGlobalConstants.Preferences.INBOX_PATH, inboxPath);
                          	
                       
                          	try {
								preferences.flush();
							} catch (BackingStoreException e1) {
								logger.error(e1);
							}
                       
                        
                         }
            	     });
            	     
            	  	subMonitor.worked(1);
             	 	 
            	  //--------------------------------------------------      
            	  	
            	  // XXX check if database exists on this place	
            	  	
                    // sleep a second
           		 subMonitor.subTask(messages.setupworker_create_database);
           		 
           		 
           	 	 Thread.sleep(500);
            	  	
					MatrosConnectionCredential dbCredentials = new MatrosConnectionCredential();
					
//					dbCredentials.setDbPath(masterData.getDbConnection().getDbPath());
					dbCredentials.setDbPasswd(masterData.getDbConnection().getDbPasswd());	
					dbCredentials.setDbUser(masterData.getDbConnection().getDbUser());
					
			
			//	 	((MatrosServerProxy) Proxy.getInvocationHandler(service)).registerNewProperties(dbCredentials);
				
            	     
              
            	   	subMonitor.worked(1);
            	     
            	  //--------------------------------------------------      
            	  	
            	
       	    	 logger.info("Database created ");
            	   	subMonitor.worked(1);
            	     
            	  //--------------------------------------------------      
            	  	
            	   	
            	  //--------------------------------------------------      
            	  	
            	  	
                    // sleep a second
           		 subMonitor.subTask(messages.setupworker_create_documentstore_filesystem);
           	 	 Thread.sleep(500);
           	 	 
           	 	 // XXX
            	 
//           	 	 for (ScannedFilesDirectory store:  masterData.getScannedFilesDirectory()) {
//           	 		
//           	   	 	 service.setScannedFilesDirectory(store);
//           	    	 logger.info("ScannedFilesDirectory " + store.getName() + " created ");
//           	   	 	 
//           	 	 }
//        	 	 
        
            	   	subMonitor.worked(1);
            	     
            	  //--------------------------------------------------      
            	   	
            	   	
            	  
               	 	 
//--------------------------------------------------            	 	  	
            	 
                	 	 
                	 
                 } catch (Exception e) {
                	 
                		Status status = new Status(IStatus.ERROR, "SetupClass", "Programming bug?", e); //$NON-NLS-2$
                		statusReporter.report(status, StatusReporter.LOG );
                		
                		error = true;
                    
                 } finally {
      					monitor.done();
      			        System.setProperty(IWorkbench.CLEAR_PERSISTED_STATE, Boolean.TRUE.toString());
                 }


	}

}
