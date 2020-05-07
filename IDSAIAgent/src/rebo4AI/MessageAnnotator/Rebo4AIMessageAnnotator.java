package rebo4AI.MessageAnnotator;

//package MTurkAgent.src.rebo4AI.MessageAnnotator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.agents.listeners.MessageAnnotator;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class Rebo4AIMessageAnnotator implements BasilicaPreProcessor {
	
	public static String GENERIC_NAME = "MessageAnnotator";
	public static String GENERIC_TYPE = "Filter";

	protected Map<String, List<String>> dictionaries = new HashMap<String, List<String>>();
	public static Map<String, List<String>> data = new HashMap<String, List<String>>();


	public Rebo4AIMessageAnnotator()
	{
		// super(a, n, pf);

		File dir = new File("dictionaries");
		
		loadDictionaryFolder(dir);
	}
	
	
	private void loadDictionaryFolder(File dir)
	{
		File[] dictNames = dir.listFiles();

		for (File dictFile : dictNames)
		{
			if (dictFile.isDirectory())
				loadDictionaryFolder(dictFile);
			else if(dictFile.getName().endsWith(".txt"))
			{
				String name = dictFile.getName().replace(".txt", "").toUpperCase();
				dictionaries.put(name, loadDictionary(dictFile));
			}
		}
	}

	private void updateAnnotations(String from, String text, String annotations, String agentName)
	{
		try
		{
			String filename = "behaviors" + File.separator + "students" + File.separator + agentName + ".studentannotations.txt";
			FileWriter fw = new FileWriter(filename, true);
			fw.write(agentName + "," + from + "," + text.replace(",", " ") + "," + annotations + "\n");
			fw.flush();
			fw.close();
		}
		catch (Exception e)
		{
			Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "Error while updating Status File (" + e.toString() + ")");
		}
	}

	private List<String> loadDictionary(File dict)
	{
		List<String> dictionary = new ArrayList<String>();
		try
		{
			BufferedReader fr = new BufferedReader(new FileReader(dict));

			String line = fr.readLine();
			while (line != null)
			{
				line = line.trim();
				if (line.length() > 0)
				{
					dictionary.add(line.trim());
				}
				line = fr.readLine();
			}
			fr.close();
		}
		catch (Exception e)
		{
			Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "Error while reading Dictionary: " + dict.getName() + " (" + e.toString() + ")");
		}
		return dictionary;
	}

	
	
	
	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		/* Behzad
		 * If the event is a MessageEvent, it will be handled by handleMessageEvent.
		 */
		if (e instanceof MessageEvent)
		{
			handleMessageEvent(source, (MessageEvent) e);
		}
	}

	private void handleMessageEvent(InputCoordinator source, MessageEvent me)
	{
		/* Behzad
		 * This function normalizes the response and tries to match it to a specific annotation.
		 * Then add the annotation to this property Map<String, List<String>> annotations in MessageEvent.java; 
		 */
		String text = me.getText();
		String normalizedText = normalize(text);
		MessageEvent newme = me;// new MessageEvent(source, me.getFrom(),
								// me.getText());

		// NORMAL ANNOTATIONS
		for (String key : dictionaries.keySet())
		{
			List<String> dictionary = dictionaries.get(key);
			List<String> namesFound = matchDictionary(normalizedText, dictionary);
			if (namesFound.size() > 0)
			{
				newme.addAnnotation(key, namesFound);
			}
		}
		

		
		//old rohitk
//		updateAnnotations(me.getFrom(), me.getText(), newme.getAnnotationString(), source.getAgent().getName());

		//Logger.commonLog(getClass().getSimpleName(), Logger.LOG_NORMAL, "annotations:" + newme.getAnnotationString());
		// source.addPreProcessingEvent(newme);
		/*
		 * Behzad's code
		 * for unk situation
		 */
		if(!Arrays.asList(newme.getAllAnnotations()).contains("IDSAI_QUESTION1_CONCEPTS")){
		    // is present ... :)
			
			List<String> matchedTerms = new ArrayList<String>();
			matchedTerms.add("without reason");
			newme.addMyAnnotation("IDSAI_QUESTION1_WITHOUT_REASON", matchedTerms);
//			newme.addMyAnnotation("QUESTION1_WITHOUT_REASON_ANNOTATION", matchedTerms);
			
		} 
	}

	public static String normalize(String text)
	{
		/* Behzad
		 * Definitely we need a better normalizer. Covering more punctuation ...
		 */
		if(text == null)
			return text;
		
		String rettext = text.replace(",", " , ");
		rettext = rettext.replace(".", " . ");
		rettext = rettext.replace("?", " ? ");
		rettext = rettext.replace("!", " ! ");
		rettext = rettext.replace("'", "'");
		rettext = rettext.replace("\"", " \" ");
		rettext = rettext.trim();
		rettext = rettext.replace("  ", " ");
		rettext = rettext.replace("  ", " ");
		rettext = rettext.replace("  ", " ");
		rettext = rettext.replace("\t", " ");
		rettext = rettext.toLowerCase();
		return rettext;
	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[] { MessageEvent.class };
	}
	
	protected List<String> matchDictionary(String text, List<String> dictionary) {

		/* Behzad:
		 * get a dictionary, in which we have all the phrases of a specific annotation, 
		 * each key is converted to a regex pattern and then look for the pattern into the response. (Flashtext??) 
		 */
		
		/* Behzad
		 * sometimes "wplanning" does not match with "\bwplanning\b". I dont know why.
		 * 
		 */
//		text = "wplanning";
//		if (text.equals("wplanning")) {
//			int a = 0;
//		}
		List<String> matchedTerms = new ArrayList<String>();
		for (int j = 0; j < dictionary.size(); j++)
		{
			String entry = dictionary.get(j);
			try
			{
				String regex = "";
				
				if (entry.startsWith("/") && entry.endsWith("/")) {
					regex = ".*" + entry.substring(1, entry.length() - 1) + ".*";
				}
				else {
					regex = "\\b" + entry + "\\b";
				}
				
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
                java.util.regex.Matcher matcher = pattern.matcher(text);
                
                if (matcher.find())
                {
                    matchedTerms.add(entry);
                }
			}
			catch (Exception e)
			{
				Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "problem matching against line " + j + ": " + entry);
			}

		}
		return matchedTerms;


  }
	
}
