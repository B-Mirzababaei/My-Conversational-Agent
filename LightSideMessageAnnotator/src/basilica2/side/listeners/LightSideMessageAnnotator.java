package basilica2.side.listeners;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.io.Console;
import java.io.File;
import java.util.Scanner;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.side.util.MultipartUtility;


public class LightSideMessageAnnotator extends BasilicaAdapter
{
	String pathToLightSide; 
	String modelName; 
	String modelNickname;
	String predictionCommand; 
	String classificationString; 
 	String host = "http://localhost";
 	//String host = "http://172.17.42.1";
//	String host = "http://kcs-rebo4ai-internal.know.know-center.at";
	String port = "8000"; 
    String charset = "UTF-8";
	String modelPath = "saved\\";
    MultipartUtility mUtil; 
    Hashtable<String, Double> classify_dict = new Hashtable<String, Double>();
	
	public LightSideMessageAnnotator(Agent a)
	{
		super(a);
		log(Logger.LOG_NORMAL, "-------------------2222-------------------- 34 ================================= ");

		port = getProperties().getProperty("port", port);

		pathToLightSide = getProperties().getProperty("pathToLightSide", pathToLightSide);
		modelPath = getProperties().getProperty("modelPath", modelPath);
		modelName = getProperties().getProperty("modelName", modelName);        
		modelNickname = getProperties().getProperty("modelNickname", modelNickname);
		predictionCommand = getProperties().getProperty("predictionCommand", predictionCommand);
		Process process;
		File lightSideLocation = new File(pathToLightSide);
		log(Logger.LOG_NORMAL, "-------------------2222-------------------- 44  ================================= ");

		classificationString = getProperties().getProperty("classifications", classificationString);
		String[] classificationList = classificationString.split(","); 
		int listLength = classificationList.length; 
		for (int i=0; i<listLength; i+=2) {
			classify_dict.put(classificationList[i],Double.parseDouble(classificationList[i+1]));
		}
		classify_dict.put("detected",Double.parseDouble("60"));
		classify_dict.put("notdetected",Double.parseDouble("60"));
		log(Logger.LOG_NORMAL, "-------------------2222-------------------- 52 ================================= ");

		try {
			log(Logger.LOG_NORMAL, "-------------------2222-------------------- 55  ================================= " + predictionCommand);
			log(Logger.LOG_NORMAL, "-------------------2222-------------------- 55  ================================= " + port);
			log(Logger.LOG_NORMAL, "-------------------2222-------------------- 55  ================================= " + modelNickname);
			log(Logger.LOG_NORMAL, "-------------------2222-------------------- 55  ================================= " + modelPath);
			log(Logger.LOG_NORMAL, "-------------------2222-------------------- 55  ================================= " + modelName);
			log(Logger.LOG_NORMAL, "-------------------2222-------------------- 55  ================================= " + lightSideLocation);
			System.err.println("--------------------pos:saved/pos.model.side-------------------------0-0-0-0-0-0-0-0-0-0-0-0-0-----------------pos:saved/pos.model.side------------------");


			ProcessBuilder pb = new ProcessBuilder(predictionCommand,port,modelNickname + ":" + modelPath + modelName);
			log(Logger.LOG_NORMAL, "-------------------2222-------------------- 64  ================================= ");

			pb.directory(lightSideLocation);
			log(Logger.LOG_NORMAL, "-------------------2222-------------------- 67  ================================= ");
			
			pb.inheritIO();
			log(Logger.LOG_NORMAL, "-------------------2222-------------------- 70  ================================= ");

			process = pb.start(); 
			log(Logger.LOG_NORMAL, "-------------------2222-------------------- 72 process = pb.start();   ================================= ");

			Boolean isAlive = process.isAlive();
			if (isAlive) {
				log(Logger.LOG_NORMAL, "-------------------2222-------------------- LightSide process is alive  ================================= ");

				System.err.println("LightSide process is alive");
			}
			else {
				log(Logger.LOG_NORMAL, "-------------------2222-------------------- LightSide process is NOT alive ================================= ");

				System.err.println("LightSide process is NOT alive");			
			}
			
		} 
		catch (Exception e)
		{
			System.err.println("LightSideMessageAnnotator, error starting LightSide");
			log(Logger.LOG_ERROR, "-------------------2222-------------------- 77 " + e);

			e.printStackTrace();
		}
		
	}

	/**
	 * @param source
	 *            the InputCoordinator - to push new events to. (Modified events
	 *            don't need to be re-pushed).
	 * @param event
	 *            an incoming event which matches one of this preprocessor's
	 *            advertised classes (see getPreprocessorEventClasses)
	 * 
	 *            Preprocess an incoming event, by modifying this event or
	 *            creating a new event in response. All original and new events
	 *            will be passed by the InputCoordinator to the second-stage
	 *            Reactors ("BasilicaListener" instances).
	 */
	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		log(Logger.LOG_NORMAL, "-------------------3333-------------------- preProcessEvent ================================= ");

		MessageEvent me = (MessageEvent) event;
		System.out.println(me);

		String text = me.getText();
		log(Logger.LOG_NORMAL, "-------------------3333------------------preProcessEvent-- " + text + " ================================= ");

		String label = annotateText(text);
		if (label != null)
		{
			log(Logger.LOG_WARNING, "-------------------7777------------------preProcessEvent-- " + label + " ================================= ");

			me.addAnnotations(label);
			
		}

	}

	
	public String annotateText(String text)
	{
		log(Logger.LOG_NORMAL, "-------------------3333-------------------- annotateText ================================= ");

		try {
			System.err.println("=== LightSideMessageAnnotator - classification " + text + " =================================");
			log(Logger.LOG_NORMAL, "-------------------3333--------------------0 annotateText ============"+ host+ "===================== ");
			log(Logger.LOG_NORMAL, "-------------------3333--------------------0 annotateText =============" + port + "==================== ");
			log(Logger.LOG_NORMAL, "-------------------3333--------------------0 annotateText =============" + modelName + "=================== ");
			log(Logger.LOG_NORMAL, "-------------------3333--------------------0 annotateText ==============" +charset + "=================== ");
			log(Logger.LOG_ERROR, "-------------------3333--------------------0 annotateText ==============" +host + ":" + port + "/try/" + modelNickname + "=================== ");

			MultipartUtility mUtil = new MultipartUtility(host + ":" + port + "/try/" + modelNickname, charset);
			log(Logger.LOG_NORMAL, "-------------------3333--------------------1 annotateText ================================= ");
		
            mUtil.addFormField("sample", text);
    		log(Logger.LOG_NORMAL, "-------------------3333--------------------2 annotateText ================================= ");

            mUtil.addFormField("model", modelPath + modelName );
			log(Logger.LOG_NORMAL, "-------------------3333--------------------3 annotateText ============"+ modelPath+ "===================== ");
            List<String> finish = mUtil.finish();
    		log(Logger.LOG_NORMAL, "-------------------3333--------------------4 annotateText ================================= ");
            StringBuilder response = new StringBuilder();
            for (String line : finish) {
                response.append(line);
                response.append('\r');
            }
    		log(Logger.LOG_NORMAL, "-------------------3333--------------------5 annotateText ================================= ");

            String classifications = parseLightSideResponse(response);
    		log(Logger.LOG_NORMAL, "-------------------3333--------------------6 annotateText ================================= ");

            return classifications; 
	    } catch (IOException e) {
	    	StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            
			log(Logger.LOG_NORMAL, "-------------------3333 outer-------------------- annotateText ====LightSide returned null============================= " + exceptionAsString);

	    	e.printStackTrace();
	    	return "LightSide returned null"; 
//	    	return "DETECTED" 
	    }	
	}
	
	public String parseLightSideResponse(StringBuilder response)
	{
		log(Logger.LOG_NORMAL, "-------------------3333-------------------- parseLightSideResponse ================================= ");
		log(Logger.LOG_NORMAL, "-------------------3333-------------------- parseLightSideResponse ========"+response.toString()+"========================= ");

		String startFlag = "<h3>";
		String endFlag = "</h3>";
		String classSplit = "%<br>";
		String withinClassSplit = ": ";
		String[] classificationSpec;
		String classification; 
		Double classificationPercent; 
		Double classificationThreshold;
		StringBuilder annotation = new StringBuilder(""); 
		String plus = ""; 
		
		int start = response.indexOf(startFlag);
		int end = response.indexOf(endFlag,start);
		String classifications = response.substring((start+4),end);
		log(Logger.LOG_NORMAL, "-------------------332222222233-------------------- parseLightSideResponse ========"+classifications+"========================= ");

		String[] classificationList = classifications.split(classSplit); 
		int listLength = classificationList.length; 
		for (int i=0; i < listLength; i++) {
			classificationSpec = classificationList[i].split(withinClassSplit);
			classification = classificationSpec[0];
			classificationPercent = Double.parseDouble(classificationSpec[1]);
			log(Logger.LOG_NORMAL, "=== LightSideMessageAnnotator - classification " + classification + " " + Double.toString(classificationPercent) + "%");

			try {
				classificationThreshold = classify_dict.get(classification);
				log(Logger.LOG_NORMAL, "=== LightSideMessageAnnotator - classificationThreshold " + classificationThreshold + "   000000000000000000000000000000%");

				if (classificationPercent >= classificationThreshold) {

					annotation.append(plus + classification.toUpperCase());
					plus = "+"; 					
				}
			}
			catch (Exception e) {
		    	System.out.println("LightSide classification \"" + classification + "\" not used"); 
			}			
		}
		return annotation.toString(); 	
	}

	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		log(Logger.LOG_NORMAL, "-------------------3333-------------------- getPreprocessorEventClasses ================================= ");

		// only MessageEvents will be delivered to this watcher.
		return new Class[] { MessageEvent.class };
	}

	public static void main(String[] args)
	{

		Scanner input = new Scanner(System.in);
		LightSideMessageAnnotator annotator = new LightSideMessageAnnotator(null);

		while (input.hasNext())
		{
			String text = input.nextLine();
			String label = annotator.annotateText(text);
			System.out.println("Label is " + label);
		}
		input.close();
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		log(Logger.LOG_NORMAL, "-------------------3333-------------------- getListenerEventClasses ================================= ");

		// no processing events.
		return new Class[]{};
	}

	@Override
	public void processEvent(InputCoordinator arg0, Event arg1)
	{
		log(Logger.LOG_NORMAL, "-------------------3333-------------------- processEvent ================================= ");

		//we do nothing
	}
	

}
