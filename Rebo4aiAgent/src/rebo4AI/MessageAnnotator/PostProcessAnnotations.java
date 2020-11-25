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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.agents.listeners.MessageAnnotator;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class PostProcessAnnotations implements BasilicaPreProcessor {
	
	public static String GENERIC_NAME = "MessageAnnotator";
	public static String GENERIC_TYPE = "Filter";

	protected Map<String, List<String>> dictionaries = new HashMap<String, List<String>>();
	public static Map<String, List<String>> data = new HashMap<String, List<String>>();
    //Behzad: for replacing the entities to ENT
	public static Map<String, String> similarNames = new HashMap<String, String>();
    //Behzad: minimum length for each part of a response
	public static Map<String, Integer> minimumLengthForEachPart = new HashMap<String, Integer>() {{
	    put("CLAIM", 3);
	    put("WARRANT", 4);
	 }};

    
    //Behzad: for stopword
    public static List<String> stopWords = Arrays.asList("a", "about", "above", "after", "again", "against", "ain", "all", "am", "an", "and", "any", "are", "aren", "aren't", "as", "at",
    		"be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can", "couldn", "couldn't", "d", "did", "didn", "didn't", "do", "does", "doesn",
    		"doesn't", "doing", "don", "don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn", "hadn't", "has", "hasn", "hasn't", "have", "haven", "haven't", 
    		"having", "he", "her", "here", "hers", "herself", "him", "himself", "his", "how", "i", "if", "in", "into", "is", "isn", "isn't", "it", "it's", "its", "itself", "just", "ll",
    		"m", "ma", "me", "mightn", "mightn't", "more", "most", "mustn", "mustn't", "my", "myself", "needn", "needn't", "no", "nor", "not", "now", "o", "of", "off", "on", "once", "only",
    		"or", "other", "our", "ours", "ourselves", "out", "over", "own", "re", "s", "same", "shan", "shan't", "she", "she's", "should", "should've", "shouldn", "shouldn't", "so", "some",
    		"such", "t", "than", "that", "that'll", "the", "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too", "under", "until",
    		"up", "ve", "very", "was", "wasn", "wasn't", "we", "were", "weren", "weren't", "what", "when", "where", "which", "while", "who", "whom", "why", "will", "with", "won", "won't", "wouldn",
    		"wouldn't", "y", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves", "could", "he'd", "he'll", "he's", "here's", "how's", "i'd", "i'll", "i'm", "i've",
    		"let's", "ought", "she'd", "she'll", "that's", "there's", "they'd", "they'll", "they're", "they've", "we'd", "we'll", "we're", "we've", "what's", "when's", "where's", "who's", "why's", "would");
    		
	public PostProcessAnnotations()
	{
		similarNames.put("scenario-idsai-google", "\\b(a|an|the)?\\s?\\b(google('s)? search engine|google)(s)?\\b");
		similarNames.put("scenario-idsai-liberty", "\\b(a|an|the)?\\s?\\b(new york statue of liberty|statue of liberty|thenew york statue of liberty|new york state of liberty|ny statue of liberty|new york status of liberty|newyork statue of liberty)(s)?\\b");
		similarNames.put("scenario-idsai-tree", "\\b(a|an|the)?\\s?\\b(tree)(s)?\\b");
		similarNames.put("scenario-idsai-monkey", "\\b(a|an|the)?\\s?\\b(google('s)? search engine|google)(s)?\\b");
		similarNames.put("scenario-idsai-sunflower", "\\b(a|an|the)?\\s?\\b(sun\\s?flower|flower)(s)?\\b");
		similarNames.put("scenario-idsai-self-driving-car", "\\b(a|an|the)?\\s?\\b(self (- )?driving car)(s)?\\b");
		similarNames.put("scenario-idsai-venus-trap", "\\b(a|an|the)?\\s?\\b(venus fly trap|venus fly\\s?(trap)?)(s)?\\b");
		similarNames.put("scenario-idsai-cat", "\\b(a|an|the)?\\s?\\b(cat)(s)?\\b");
		
		
		
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
	

	public String replaceEntity(String text, String dialog_name)
	{
		if (similarNames.containsKey(dialog_name)) {
			text = text.replaceAll(similarNames.get(dialog_name), " ENT ");
			text = text.replaceAll(" ( )+", " ");

		}
		
		return text.trim();
	}
	private int hasEvidence_rulebased(String normalizedText, List<String> all_annotations)
	{
		//BEHZAD: predict the evidence based on the length of resonses
		int word_number = normalizedText.replaceAll("[^a-z ]", "").split("\\s+").length;
		if (word_number <= 5) {
			return word_number;
		}
		if (all_annotations.contains("CLAIM_POS") || 
				all_annotations.contains("IDSAI_QUESTION1_IS_INTELLIGENT")||
				all_annotations.contains("CLAIM_NEG")||
				all_annotations.contains("IDSAI_QUESTION1_IS_NOT_INTELLIGENT")) {
			word_number-= minimumLengthForEachPart.get("CLAIM");
			
		}
		if (all_annotations.contains("WARRANT_WITH") || 
				all_annotations.contains("IDSAI_QUESTION1_WITH_WARRANT")) {
			word_number-= minimumLengthForEachPart.get("WARRANT");
		}
		
//		for (String label : all_annotations) {
//			if (minimumLengthForEachPart.containsKey(label)) {
//				word_number-= minimumLengthForEachPart.get(label);
//			}
//		}
		return word_number;
//		if (word_number <= 5) {
//			return "WITHOUT_EVIDENCE";
//		}
//		return "WITH_EVIDENCE";
	}

	private void handleMessageEvent(InputCoordinator source, MessageEvent me)
	{
		MessageEvent newme = me;// new MessageEvent(source, me.getFrom(),
		/* Behzad
		 * predict having evidence based on length of a response and its annotations
		 */
		String text = me.getText();
		String normalizedText = cleanText(text);
		normalizedText = replaceEntity(normalizedText, source.dialog_name);

		
		// =============================check based on length============================
		int word_number = hasEvidence_rulebased(normalizedText, Arrays.asList(newme.getAllAnnotations()));
		
		String evidence_label = "";
		
		if (word_number <= 5) {
			 if (Arrays.asList(newme.getAllAnnotations()).contains("WITH_EVIDENCE") ) { 
				newme.removeAnnotation("WITH_EVIDENCE");
				newme.addMyAnnotation("WITHOUT_EVIDENCE", Arrays.asList("this is for the evidence"));
			 }		
			 evidence_label = "WITHOUT_EVIDENCE";
		}
		else {
			if (Arrays.asList(newme.getAllAnnotations()).contains("WITH_EVIDENCE") ) { 
				evidence_label = "WITH_EVIDENCE";
			}
			else {
				evidence_label = "WITHOUT_EVIDENCE";
			}
		}
		
		//=============================END check based on length============================
//		newme.addMyAnnotation(evidence_label, Arrays.asList("this is for the evidence"));
		
		
		
		//===========================================================================
		
		// NORMAL ANNOTATIONS
//		for (String key : dictionaries.keySet())
//		{
//			List<String> dictionary = dictionaries.get(key);
//			List<String> namesFound = matchDictionary(normalizedText, dictionary);
//			if (namesFound.size() > 0)
//			{
//				newme.addAnnotation(key, namesFound);
//			}
//		}
		

		String annotation = "CLAIM_NONE";
		List<String> matchedTerms = new ArrayList<String>();
		matchedTerms.add("sth");
		
		if (Arrays.asList(newme.getAllAnnotations()).contains("CLAIM_POS") || Arrays.asList(newme.getAllAnnotations()).contains("IDSAI_QUESTION1_IS_INTELLIGENT")) {
			annotation= "POS_CLAIM";
			
		}
		else if (Arrays.asList(newme.getAllAnnotations()).contains("CLAIM_NEG") || Arrays.asList(newme.getAllAnnotations()).contains("IDSAI_QUESTION1_IS_NOT_INTELLIGENT")) {
			annotation= "NEG_CLAIM";
		}
		if (!annotation.equals("CLAIM_NONE")) {
			if (Arrays.asList(newme.getAllAnnotations()).contains("WARRANT_WITH") || Arrays.asList(newme.getAllAnnotations()).contains("IDSAI_QUESTION1_WITH_WARRANT")) {
				annotation +="__WITH_WARRANT";	
				annotation += "__"+evidence_label;
			}
			else {
				annotation+="__WITHOUT_WARRANT";
			}
			newme.addAnnotation(annotation, matchedTerms);
			if (Arrays.asList(newme.getAllAnnotations()).contains("CLAIM_NONE")){
				newme.removeAnnotation("CLAIM_NONE");
				
			}
		}
	}

	public String cleanText(String text)
	{
		// ([.":,;\-\)\(!?]) => \s\1\s
	   if(text == null)
			return text;
		
		String rettext = text.replaceAll("([.\":,\\[\\]\\{\\}_\\\\\\/;\\-\\)\\(!?])", " $1 ");
//		rettext = rettext.replace(".", " . ");
//		rettext = rettext.replace("?", " ? ");
//		rettext = rettext.replace("!", " ! ");
//		rettext = rettext.replace(":", " : ");
//		rettext = rettext.replace("\"", " \" ");
//		rettext = rettext.replace("-", " - ");
//		rettext = rettext.replace(")", " ) ");
//		rettext = rettext.replace("\"", " \" ");
//		rettext = rettext.replace("\"", " \" ");
//		
		rettext = rettext.replace("’", "'");

		rettext = rettext.replaceAll(" ( )+", " ");
//		rettext = rettext.replace("  ", " ");
//		rettext = rettext.replace("  ", " ");
//		rettext = rettext.replace("\t", " ");
		rettext = rettext.toLowerCase();
		rettext = rettext.trim();
		
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
