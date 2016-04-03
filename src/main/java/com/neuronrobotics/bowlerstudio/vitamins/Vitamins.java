package com.neuronrobotics.bowlerstudio.vitamins;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.neuronrobotics.imageprovider.NativeResource;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.STL;

import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.vitamins.Vitamins;
import javafx.scene.paint.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
public class Vitamins {
	
	private static final Map<String,CSG> fileLastLoaded = new HashMap<String,CSG>();
	private static final Map<String,HashMap<String,HashMap<String,Object>>> databaseSet = 
			new HashMap<String, HashMap<String,HashMap<String,Object>>>();
	private static String gitRpoDatabase = "https://github.com/madhephaestus/Hardware-Dimensions.git";
	//Create the type, this tells GSON what datatypes to instantiate when parsing and saving the json
	private static Type TT_mapStringString = new TypeToken<HashMap<String,HashMap<String,Object>>>(){}.getType();
	//chreat the gson object, this is the parsing factory
	private static Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	public static CSG get(String resource ){
		if(fileLastLoaded.get(resource) ==null ){
			// forces the first time the files is accessed by the application tou pull an update
			try {
				fileLastLoaded.put(resource,STL.file(NativeResource.inJarLoad(IVitamin.class,resource).toPath()) );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return fileLastLoaded.get(resource).clone() ;
	}
	public static CSG get(File resource ){
		
		if(fileLastLoaded.get(resource.getAbsolutePath()) ==null ){
			// forces the first time the files is accessed by the application tou pull an update
			try {
				fileLastLoaded.put(resource.getAbsolutePath(), STL.file(resource.toPath()) );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return fileLastLoaded.get(resource.getAbsolutePath()).clone() ;
	}
	
	public static CSG get(String type,String id) throws Exception{
		
		if(fileLastLoaded.get(type+id) ==null ){
			CSG newVitamin=null;
			HashMap<String, Object> script = getScript( type);
			ArrayList<Object> servoMeasurments = new ArrayList<Object>();
			servoMeasurments.add(id);
			newVitamin=(CSG)ScriptingEngine
            .gitScriptRun(
            		script.get("git").toString(), // git location of the library
            		script.get("file").toString(), // file to load
                      servoMeasurments
            );
			
			fileLastLoaded.put(type+id, newVitamin );

		}
		return fileLastLoaded.get(type+id).clone() ;
	}
	
	
	
	public static HashMap<String, Object> getScript(String type){
		return getConfiguration(type,"script");
	}
	public static void setScript(String type, String git, String file) throws Exception{
		newParameter(type,"script","git",git);
		newParameter(type,"script","file",file);
	}
	public static HashMap<String, Object> getConfiguration(String type,String id){
		HashMap<String, HashMap<String, Object>> database = getDatabase(type);
		if(database.get(id)==null){
			database.put(id, new  HashMap<String, Object>());
		}
		return getDatabase(type).get(id);
	}
	
	public static void saveDatabase(String type) throws Exception{
		
		// Save contents and publish them
		String jsonString = gson.toJson(getDatabase( type), TT_mapStringString); 
		try{
			ScriptingEngine.pushCodeToGit(
				gitRpoDatabase,// git repo, change this if you fork this demo
				"master", // branch or tag
				"json/"+type+".json", // local path to the file in git
				jsonString, // content of the file
				"Pushing changed Database");//commit message
			
		}catch(org.eclipse.jgit.api.errors.TransportException ex){
			System.out.println("You need to fork "+gitRpoDatabase+" to have permission to save");
			System.out.println("You do not have permission to push to this repo, change the GIT repo to your fork with setGitRpoDatabase(String gitRpoDatabase) ");
			throw ex;
		}

	}
	
	public static void newVitamin(String type, String id) throws Exception{
		HashMap<String, HashMap<String, Object>> database = getDatabase(type);
		if(database.keySet().size()>0){
			String exampleKey =null;
			for(String key: database.keySet()){
				if(!key.contains("script")){
					exampleKey=key;
				}
			}
			if(exampleKey!=null){
				// this database has examples, load an example
				HashMap<String, Object> exampleConfiguration = getConfiguration(type,exampleKey);
				HashMap<String, Object> newConfig = getConfiguration( type, id);
				for(String key: exampleConfiguration.keySet()){
					newConfig.put(key, exampleConfiguration.get(key));
				}
			}
		}
		
		getConfiguration( type, id);
		saveDatabase(type);
		
	}
	
	public static void newParameter(String type, String id, String parameterName, Object parameter) throws Exception{
		
		HashMap<String, Object> config = getConfiguration( type, id);
		config.put(parameterName, parameter);
		saveDatabase(type);
	}
	
	public static HashMap<String,HashMap<String,Object>> getDatabase(String type){
		if(databaseSet.get(type)==null){
			// we are using the default vitamins configuration
			//https://github.com/madhephaestus/Hardware-Dimensions.git
	
			// create some variables, including our database
			String jsonString;
			InputStream inPut = null;
	
			// attempt to load the JSON file from the GIt Repo and pars the JSON string
			File f;
			try {
				f = ScriptingEngine
										.fileFromGit(
												gitRpoDatabase,// git repo, change this if you fork this demo
											"json/"+type+".json"// File from within the Git repo
										);
				inPut = FileUtils.openInputStream(f);
				
				jsonString= IOUtils.toString(inPut);
				// perfoem the GSON parse
				HashMap<String,HashMap<String,Object>> database=gson.fromJson(jsonString, TT_mapStringString);
	
				databaseSet.put(type, database);
				
			} catch (Exception e) {
				databaseSet.put(type, new HashMap<String,HashMap<String,Object>>());
			}
		}
		return databaseSet.get(type);

	}
	
	public static ArrayList<String> listVitaminTypes(){
		
		ArrayList<String> types = new ArrayList<String>();
		File folder;
		try {
			folder = ScriptingEngine
					.fileFromGit(
							gitRpoDatabase,// git repo, change this if you fork this demo
						"json"// File from within the Git repo
					);
			File[] listOfFiles = folder.listFiles();
			
			for(File f:listOfFiles){
				if(!f.isDirectory())
					types.add(f.getName().split(".")[0]);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return types;
	}
	
	public static ArrayList<String> listVitaminSizes(String type){
		
		ArrayList<String> types = new ArrayList<String>();
		HashMap<String, HashMap<String, Object>> database = getDatabase( type);
		Set<String> keys = database.keySet();
		for(String s:keys){
			if(!s.contains("script"))
				types.add(s);
		}
		
		return types;
	}
	
	
	public static String getGitRpoDatabase() {
		return gitRpoDatabase;
	}
	public static void setGitRpoDatabase(String gitRpoDatabase) {
		Vitamins.gitRpoDatabase = gitRpoDatabase;
	}
	
	
	
}
