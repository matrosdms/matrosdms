package net.schwehla.matrosdms.persistenceservice.internal.cryptprovider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Objects;

import net.schwehla.matrosdms.domain.core.Identifier;
import net.schwehla.matrosdms.persistenceservice.internal.StoreResult;
import net.schwehla.matrosdms.persistenceservice.internal.cryptprovider.externalcommand.ExternalCommand;
import net.schwehla.matrosdms.persistenceservice.internal.cryptprovider.externalcommand.ExternalCommandResult;
import net.schwehla.matrosdms.rcp.MatrosServiceException;

public class MatrosExternalCryptor extends AbstractMatrosCryptor implements IMatrosStoreCryptor {


	String password;
	String pathToExe;
	String cryptLine;
	String uncryptLine;
	
	public MatrosExternalCryptor(Path localPath) {
		super(localPath);
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getPathToExe() {
		return pathToExe;
	}
	public void setPathToExe(String pathToExe) {
		this.pathToExe = pathToExe;
	}
	public String getCryptLine() {
		return cryptLine;
	}
	public void setCryptLine(String cryptLine) {
		this.cryptLine = cryptLine;
	}
	public String getUncryptLine() {
		return uncryptLine;
	}
	public void setUncryptLine(String uncryptLine) {
		this.uncryptLine = uncryptLine;
	}
	


    
	@Override
	public StoreResult persist(File droppedFile, Identifier identifier) throws MatrosServiceException {
		
		StoreResult result = new StoreResult();
		
		File finalFileName = buildFinalStoredElementFilename(identifier);
		File finalFileNameParent = finalFileName.getParentFile();

		if (!finalFileNameParent.exists() && ! finalFileNameParent.mkdirs()) {
			throw new MatrosServiceException("cannot create directory " + finalFileNameParent.getAbsolutePath());
		}
		
	// 	Command pack = new Command( "-mhe=on" , "a", "destination2.7z" ,  "C:\\temp\\mittwoch.jar" ,  "-pSECTRET"  ) {
        ExternalCommand pack = new ExternalCommand( "-mhe=on" , "a" , "-aoa" ,  finalFileName.getName() , "\"" + droppedFile.getAbsolutePath() + "\"",  "-p"  + password  ) {

               @Override
               protected File directory() {
                     return buildFinalStoredElementFilename(identifier).getParentFile().getAbsoluteFile();
               }

               @Override
               protected String command() {
                     return pathToExe;
               }

        };

       
        
        try {
        	ExternalCommandResult externalResult = pack.execute();

        	
        	if (Objects.isNull(externalResult) || externalResult.code != 0) {
        		throw new MatrosServiceException("Returncode != 0 " + externalResult.output);
        	}
        	
            String sha = getSHA256(finalFileName);
            
            result.setSHA256(sha);
            result.setCryptSettings("7z");

            
		} catch (Exception e) {
			throw new MatrosServiceException("Error packing: " + e , e);
		}
          
	
		return result;
		
		
		
		
	}
	
	@Override
	public FileInputStream getStreamedContent(Identifier identifier) throws MatrosServiceException {
	
		File finalFileName = buildFinalStoredElementFilename(identifier);
		
	       try {
	    	   
				File tmp = new File (configReader.getApplicationCacheDir());
				tmp.mkdirs();
				
				


			    //    Command command = new Command( "e", "destination2.7z" , "-aoa" , "-oc:\\temp\\unpacked.vbs" ,  "-pSECTRET"  ) {

					// TODO: c:\temp is hardcoded
			   
			        ExternalCommand unpack = new ExternalCommand(   "e" , "-aoa" ,  finalFileName.getAbsolutePath() 
			        		, "-o" + tmp.getAbsolutePath() + File.separator + identifier.getUuid()  ,  "-p"  + password  ) {

			            @Override
			            protected File directory() {
			                  return tmp;
			            }

			            @Override
			            protected String command() {
			                  return pathToExe;
			            }

			        };

			      	
			     //  	System.out.println(unpack);
			        	ExternalCommandResult externalResult = unpack.execute();
			         	
			         	if (Objects.isNull(externalResult) || externalResult.code != 0) {
			         		throw new MatrosServiceException("Returncode != 0 " + externalResult.output);
			         	}
			         	
			         	
			         	File root = new File(tmp + File.separator + identifier.getUuid() );
			         	
			         	File testDirectory = root;
			         	File[] files = testDirectory.listFiles();
			         	
			         	if (files.length == 1) {
			         		
			         		// File is extracted
			         		
			         		FileInputStream fos = new FileInputStream(files[0]);
			         		return fos;
			         		
			         		
			         	}
			         	
			         	throw new RuntimeException("Cannot extract: " + identifier);
			         	
			         	
			         
				
	       }
	       
    catch(Exception e) {
    	
    	if (e instanceof MatrosServiceException) {
    		throw (MatrosServiceException) e;
    	}
    	
    	throw new RuntimeException(e);
    }
    	
    
		
	}
	
    


	
	@Override
	public File getStoredElementFile(Identifier identifier) {
		return buildFinalStoredElementFilename(identifier);
	}
	

	private File buildFinalStoredElementFilename(Identifier identifier) {
		
		File fItem = new File( cloudRoot.toFile().getAbsolutePath() + File.separator + identifier.getUuid().toLowerCase().substring(0, 2)
				+  File.separator + identifier.getUuid().toLowerCase() + ".7z");
		return fItem;
	}


}
