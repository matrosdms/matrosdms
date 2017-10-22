package net.schwehla.matrosdms.rcp.parts.helper;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.inject.Inject;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import net.schwehla.matrosdms.domain.core.InfoItem;
import net.schwehla.matrosdms.persistenceservice.IMatrosServiceService;
import net.schwehla.matrosdms.persistenceservice.internal.MatrosConfigReader;
import net.schwehla.matrosdms.rcp.MatrosServiceException;
import net.schwehla.matrosdms.rcp.MyGlobalConstants;

@Creatable
public class DesktopHelper  {
	
	@Inject
	@Preference(nodePath = MyGlobalConstants.Preferences.NODE_COM_MATROSDMS) 
	IEclipsePreferences preferences ;
    
    
	@SuppressWarnings("restriction")
	@Inject Logger logger;

	// XXX
	MatrosConfigReader configReader = new MatrosConfigReader();
	
	@Inject 
	IMatrosServiceService service;
	
    // http://www.java-forums.org/new-java/69787-cant-open-shared-server-folder-spaces-name.html
    public  void openUrl(String path) {
		
		if (Desktop.isDesktopSupported()) {
		    try {
		      	
		    	// Window only 
		        File myFile = new File( path );
		        
		        String url = myFile.toURI().toURL().toExternalForm();
		        url = url.replace(" ", "%20");  //$NON-NLS-1$//$NON-NLS-2$
		        
		        URI u = new URI(url);
		        
		         Desktop desktop = Desktop.getDesktop();
		         desktop.browse(u);
		          
		    } catch (Exception ex) {
		    	logger.error(ex, "ERR_000001: CANNOT OPEN FILE: " + path); //$NON-NLS-1$
		    }
		}
		
	}
    

    /**
     * Streaming to local clone
     * @param doc
     * @return
     * @throws MatrosServiceException
     */
	public  String getLocallink(InfoItem doc) throws MatrosServiceException {

		
	      // XXX
	      try  (FileInputStream fos = service.getStreamedContent(doc.getIdentifier() )) {
	    	  
	    		File tempFoder = new File (configReader.getApplicationCacheDir() + File.separator + "remotecopy"); //$NON-NLS-1$
				tempFoder.mkdirs();
	  
				String filename = doc.getIdentifier().getUuid() + "_" + doc.getMetadata().getFilename(); //$NON-NLS-1$
				
				File total = new File(tempFoder.getAbsolutePath() + File.separator + filename);
				
				if (!total.exists()) {
					
					Files.copy(fos,  total.toPath(), StandardCopyOption.REPLACE_EXISTING);
					total.deleteOnExit();
			
				}
		      
				return total.getAbsolutePath();
				
	      } catch (Exception ex) {
	    	  throw new MatrosServiceException("ERR_000002: CANNOT STREAM FILE TO TEMP FOLDER " + ex); //$NON-NLS-1$
	      }
		
		}
	
	
    /**
     * Creates a temp file for opening in the folder localcopy
     * @param file
     * @return
     * @throws Exception
     */
	public String getInboxNonBlockingLink(File file) throws Exception   {
		

		try {
			
			File tempFoder = new File (configReader.getApplicationCacheDir() + File.separator + "localcopy"); //$NON-NLS-1$
			tempFoder.mkdirs();
			
			String filename = file.lastModified() + "_" +file.hashCode() + "_" +  file.getName();  //$NON-NLS-1$//$NON-NLS-2$
			
			File total = new File(tempFoder.getAbsolutePath() + File.separator + filename);
			
			if (!total.exists()) {
				Files.copy(file.toPath(), total.toPath(), StandardCopyOption.REPLACE_EXISTING);
				total.deleteOnExit();
			}
			
			return total.getAbsolutePath();
			
		} catch (Exception e) {
			logger.error(e, "ERR_000003: CANNOT OPEN FILE: " + file.getName()); //$NON-NLS-1$
			throw e;
		}

		        
	}

	public static String getExtension(String filename) {
					
			String extension = ""; //$NON-NLS-1$
			String path = filename;
			
			int lastSeparator =  path.lastIndexOf(File.separator);
			
			if (lastSeparator > 0) {
				path = filename.substring(lastSeparator+1);
			}

			int i = path.lastIndexOf('.');
			if (i > 0) {
			    extension = path.substring(i+1);
			}
			
			if (extension.length() > 0) {
				extension = "." + extension; //$NON-NLS-1$
			}
			
			return extension;
		}


	public void moveFromInbox(File file) throws MatrosServiceException {

		try {
			String inboxNotProcessed =  preferences.get(MyGlobalConstants.Preferences.NOT_PROCESSED_PATH, "" ); //$NON-NLS-1$
			inboxNotProcessed = inboxNotProcessed.replaceAll(";","");  //$NON-NLS-1$//$NON-NLS-2$
			
			if (inboxNotProcessed == null || inboxNotProcessed.trim().length() == 0) {
				throw new MatrosServiceException("ERR_000004: NO PROCESSED-FOLDER DEFINED, GO TO THE PREFERENCES"); //$NON-NLS-1$
			}
			
			
			File targetProcessed = new File(inboxNotProcessed);
				
				if(! targetProcessed.exists()) {
					
					boolean create = targetProcessed.mkdirs();
					
					if (!create) {
						throw new MatrosServiceException("ERR_000005: cannot create processed folder"); //$NON-NLS-1$
					}

				}
				
			if(!file.exists()) {
				throw new MatrosServiceException("ERR_000006: Source file not exists anymore"); //$NON-NLS-1$
			}
			
			Path moveSourcePath = file.toPath() ;
			
			File target = new File( inboxNotProcessed + File.separator + file.getName() ) ;
			
			if (target.exists()) {
				target = new File(inboxNotProcessed + File.separator + System.currentTimeMillis() + "_" + file.getName() ); //$NON-NLS-1$
			}
			
			Path moveTargetPath = target.toPath();
			moveTargetPath.getParent().toFile().mkdirs();
			
			Files.move( moveSourcePath, moveTargetPath );
			
			if(! moveTargetPath.toFile().exists()) {
				throw new MatrosServiceException("ERR_000007: targetfile not created " +  moveTargetPath.getFileName()); //$NON-NLS-1$
			}
			
			if( moveSourcePath.toFile().exists()) {
				throw new MatrosServiceException("ERR_000008: sourcefile not moved " +  moveSourcePath.getFileName()); //$NON-NLS-1$
			}
			


			
		} catch (MatrosServiceException e1) {
			throw e1;
		} catch (Exception e) {
			throw new MatrosServiceException("ERR_000009: Error moving file" + e); //$NON-NLS-1$
		}
		
		
	}

	
}
