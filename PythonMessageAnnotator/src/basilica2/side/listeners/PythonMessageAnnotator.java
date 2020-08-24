package basilica2.side.listeners;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.io.Console;
import java.io.File;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.side.util.MultipartUtility;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PythonMessageAnnotator extends BasilicaAdapter
{
	
	public PythonMessageAnnotator(Agent a)
	{
		super(a);
		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            constructor ================================= ");

	}

    public String warrant(String text) throws UnirestException, ParseException {
        String lable = null;
        // TODO: Handle connection problems etc.
		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            before request ================================= ");

//        HttpResponse<String> response = Unirest.post("http://172.17.0.1:8030/warrant")
        HttpResponse<String> response = Unirest.post("http://127.0.0.1:8030/warrant")

                .basicAuth("behzad", "goorj")
                .queryString("text", text)
                .asString();
		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request ================================= ");
        
        // TODO: A better alternative?
//                .ifSuccess(response -> {
//                    try {
//                        resultBodyJson = (JSONObject) new JSONParser().parse(response.getBody());
//                    } catch (ParseException e) {
//                        e.printStackTrace();
//                    }
//                })
//                .ifFailure(response -> {
//                    System.out.println("Error " + response.getStatus());
//                    response.getParsingError().ifPresent(e -> {
//                        System.out.println("Parsing exception: " + e);
//                        System.out.println("Original body: " + e.getOriginalBody());
//                    });
//                });

//        Unirest.shutDown();

        if(response.getStatus() == 200) {
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request 2000000 ================================= ");

            JSONObject resultBodyJson = (JSONObject) new JSONParser().parse(response.getBody());
            lable = resultBodyJson.get("lable").toString();
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request label is =========== "+lable+" ====================== ");

        } else {
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request not 20000000================================= ");
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request not 20000000============== " + response.getStatus() + " ― Response: " + response.getBody() + "=================== ");

    		
            throw new UnirestException("OCR API: Error " + response.getStatus() + " ― Response: " + response.getBody());
        }
        return lable;
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
		MessageEvent me = (MessageEvent) event;

		String text = me.getText();
		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            cleaning the text ================================= ");
		try {
			
			String lable = warrant(text);
            me.addAnnotations(lable);
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            the label is added to the list ================================= ");


		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            getPreprocessorEventClasses ================================= ");

		// only MessageEvents will be delivered to this watcher.
		return new Class[] { MessageEvent.class };
	}

	public static void main(String[] args)
	{

	
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            getListenerEventClasses ================================= ");

		// no processing events.
		return new Class[]{};
	}

	@Override
	public void processEvent(InputCoordinator arg0, Event arg1)
	{
		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            processEvent ================================= ");

		//we do nothing
	}
}
