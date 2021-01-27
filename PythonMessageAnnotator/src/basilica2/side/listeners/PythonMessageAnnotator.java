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
//	public static String ip = "http://172.17.0.1"; // http://127.0.0.1 
	public static String ip = "http://127.0.0.1"; // 

	public static String port = "8030";
	public static String warrant = "/v1/warrant";
	public static String claim = "/v1/claim";
	public static String evidence = "/v1/evidence";

	
	public PythonMessageAnnotator(Agent a)
	{
		super(a);
		ip = getProperties().getProperty("ip", ip);
		port = getProperties().getProperty("port", port);


		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            constructor ================================= ");
		

	}

    public String warrant(String text) throws UnirestException, ParseException {
        String label = null;
        // TODO: Handle connection problems etc.
		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            before request ================================= ");

        
//        HttpResponse<String> response = Unirest.post("http://127.0.0.1:8030/v1/warrant")
		HttpResponse<String> response = Unirest.post(ip+":"+port+warrant)
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
            label = resultBodyJson.get("label").toString();
            if (label.equals("[1]")) {
            	label = "WITH_WARRANT";
			} else {
				label = "WITHOUT_WARRANT";
			}
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request label is =========== "+label+" ====================== ");

        } else {
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request not 20000000================================= ");
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request not 20000000============== " + response.getStatus() + " ― Response: " + response.getBody() + "=================== ");

    		
            throw new UnirestException("OCR API: Error " + response.getStatus() + " ― Response: " + response.getBody());
        }
        return label;
    }
    public String claim(String text) throws UnirestException, ParseException {

        String label = null;
        // TODO: Handle connection problems etc.
		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            before request ================================= ");

//        HttpResponse<String> response = Unirest.post("http://172.17.0.1:8030/warrant")
        HttpResponse<String> response = Unirest.post(ip+":"+port+claim)

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
            label = resultBodyJson.get("label").toString();
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request label is =========== "+label+" ====================== ");
    		if (label.equals("[1]")) {
             	label = "POS_CLAIM";
 			} else if (label.equals("[-1]")) {
 				label = "NEG_CLAIM";
 			}
 			else {
 				label = "UNK_CLAIM";
 			}
 				
        } else {
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request not 20000000================================= ");
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request not 20000000============== " + response.getStatus() + " ― Response: " + response.getBody() + "=================== ");

    		
            throw new UnirestException("OCR API: Error " + response.getStatus() + " ― Response: " + response.getBody());
        }
        return label;
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
    public String evidence(String text, String label_claim, String label_warrant) throws UnirestException, ParseException {
        String label = null;
        // TODO: Handle connection problems etc.
		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            before request ================================= ");
		
        if (label_claim.equals("UNK_CLAIM")) {
        	label_claim = "0";
		} else {
			label_claim = "1";	
		}
        if (label_warrant.equals("WITH_WARRANT")) {
        	label_warrant = "1";
		} else {
			label_warrant = "0";	
		}
//        HttpResponse<String> response = Unirest.post("http://127.0.0.1:8030/v1/warrant")
		HttpResponse<String> response = Unirest.post(ip+":"+port+evidence)
                .basicAuth("behzad", "goorj")
                .queryString("text", text)
                .queryString("claim", label_claim)
                .queryString("warrant", label_warrant)
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
            label = resultBodyJson.get("label").toString();
            if (label.equals("[1]")) {
            	label = "WITH_EVIDENCE";
			} else {
				label = "WITHOUT_EVIDENCE";
			}
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request label is =========== "+label+" ====================== ");

        } else {
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request not 20000000================================= ");
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            after request not 20000000============== " + response.getStatus() + " ― Response: " + response.getBody() + "=================== ");

    		
            throw new UnirestException("OCR API: Error " + response.getStatus() + " ― Response: " + response.getBody());
        }
        return label;
    }
    
    @Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		MessageEvent me = (MessageEvent) event;

		String text = me.getText();
		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            cleaning the text ================================= ");
		try {
			
			
			String label_claim = claim(text);
            me.addAnnotations(label_claim);
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            the label of claim is added to the list ================================= ");

			String label_warrant = warrant(text);
            me.addAnnotations(label_warrant);
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            the label of warrant is added to the list ================================= ");
    		
    		String label_evidence = evidence(text,label_claim, label_warrant );
            me.addAnnotations(label_evidence);
    		log(Logger.LOG_NORMAL, "-------------------8888-------------------- PYTHON            the label of claim is added to the list ================================= ");

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
