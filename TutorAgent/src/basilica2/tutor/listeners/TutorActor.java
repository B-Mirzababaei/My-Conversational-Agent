/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package basilica2.tutor.listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.PromptEvent;
import basilica2.agents.events.priority.BlacklistSource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaAdapter;
//import basilica2.social.data.TurnCounts;
import basilica2.tutor.events.DoTutoringEvent;
import basilica2.tutor.events.DoneTutoringEvent;
import basilica2.tutor.events.MoveOnEvent;
import basilica2.tutor.events.StudentTurnsEvent;
import basilica2.tutor.events.TutorTurnsEvent;
import basilica2.tutor.events.TutoringStartedEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import edu.cmu.cs.lti.tutalk.script.Concept;
import edu.cmu.cs.lti.tutalk.script.Response;
import edu.cmu.cs.lti.tutalk.script.Scenario;
import edu.cmu.cs.lti.tutalk.slim.EvaluatedConcept;
import edu.cmu.cs.lti.tutalk.slim.FuzzyTurnEvaluator;
import edu.cmu.cs.lti.tutalk.slim.TuTalkAutomata;

/**
 * 
 * @author rohitk --> dadamson --> gtomar
 */
public class TutorActor extends BasilicaAdapter implements TimeoutReceiver
{
	private boolean isTutoring = false;
	private boolean expectingResponse = false;
	private boolean nullExpected = false;
	private TuTalkAutomata currentAutomata = null;
	private String currentConcept = null;
	private int noMatchingResponseCount = 0;
	private List<String> answerers = new ArrayList<String>();
	private Map<String, Dialog> pendingDialogs = new HashMap<String, Dialog>();
	private Map<String, Dialog> proposedDialogs = new HashMap<String, Dialog>();
	private InputCoordinator source;
	
	private double tutorMessagePriority = 0.75;
	private boolean interruptForNewDialogues = false;
	private boolean startAnyways = true;
	private String dialogueFolder = "dialogs";
	
	private String dialogueConfigFile = "dialogues/dialogues-config.xml";
	private int introduction_cue_timeout = 600;
	private int introduction_cue_timeout2 = 600;
	private int tutorTimeout = 120;
	private String request_poke_prompt_text = "I am waiting for your response to start. Please ask for help if you are stuck.";
	private String goahead_prompt_text = "Let's go ahead with this.";
	private String response_poke_prompt_text = "Can you rephrase your response?";
	private String dont_know_prompt_text = "Anybody?";
	private String moving_on_text = "Okay, let's move on.";
	private String tutorialCondition = "tutorial";
	private int numUser = 1;


	class Dialog
	{

		public String conceptName;
		public String scenarioName;
		public String introText;
		public String acceptAnnotation;
		public String acceptText;
		public String cancelAnnotation;
		public String cancelText;
		public Map<String, List<String>> randomElements = new HashMap<String, List<String>>();
		public Map<String, String> htmlTagElements = new HashMap<String, String>();
		public Map<String, List<String>> knowledge_base = new HashMap<String, List<String>>();

		public Map<String, String> selectedRandomElement = new HashMap<String, String>();

		public Dialog(String conceptName, String scenarioName, String introText, String cueAnnotation, String cueText, String cancelAnnotation, String cancelText)
		{
			this.conceptName = conceptName;
			this.scenarioName = scenarioName;
			this.introText = introText;
			this.acceptAnnotation = cueAnnotation;
			this.acceptText = cueText;
			this.cancelAnnotation = cancelAnnotation;
			this.cancelText = cancelText;
			
			/* Behzad: It is about adding random elements in whatever the agent wants to say. In this version, a phrase is selected randomly when a Dialog object wants to be created.
			 * If you want to add random elements in a specific scenario, follow these steps:
			 * 1. create this folder: runtime/data
			 * 2. create this folder: runtime/data/<conceptName of scenario> 
			 * 3. create this file: runtime/data/<conceptName of scenario>/<use a string that you want to replace it with random elements>.txt 
			 *     3.1 the name of txt file should be lower case and the name in scenario file should be upper case
			 *     3.2 each row of this txt file contain a phrase
			 *     
			 *      
			 * For example: 
			 * 	This is my random file: runtime/data/IDSAI/random_elements.txt in which each line has a phrase 
			 *     a fish
			 *     a baby
			 *     a table
			 *     ...
			 *     
			 *  and in the scenario file scenario-idsai.xml, in concepts tag we have:
			 *  <concept label="question1">
			 *		<phrase>Is RANDOM_ELEMENT intelligent or not? Why? </phrase>
			 *	</concept>
			 */ 
			File folder = new File("data/" + conceptName);
			if (folder.exists() && folder.isDirectory()) {
				loadDictionaryFolder(folder);
				for (Map.Entry<String,List<String>> entry : randomElements.entrySet()) {
					String key= entry.getKey();
					
					if (key.equals("HTML_TAGS")) {
						List<String> values = entry.getValue();
						for (String tag : values) {
							htmlTagElements.put(tag.split("\t")[0], tag.split("\t")[1]);	
						}
						
					}
					if (key.equals("RANDOM_EXAMPLE")) {
						List<String> values = entry.getValue();
						selectedRandomElement.put(key, values.get((int) Math.floor(values.size() * Math.random())));
					}
					if (key.equals("KNOWLEDGE_BASE")) {
						List<String> values = entry.getValue();
						for (String tag : values) {
							if (tag.startsWith("=")) {
								continue;
							}
							List<String> items = Arrays.asList(tag.split("\t"));
							knowledge_base.put(items.get(0), items.subList(1, items.size()));
						}
					}
					
					
			
				}
			}
			for (Map.Entry<String,String> entry : selectedRandomElement.entrySet()) {
				this.introText = this.introText.replace(entry.getKey(), entry.getValue());
				this.cancelText = this.cancelText.replace(entry.getKey(), entry.getValue());
				this.acceptText = this.acceptText.replace(entry.getKey(), entry.getValue());
			}
			for (Map.Entry<String,String> entry : htmlTagElements.entrySet()) {
				this.introText = this.introText.replace(entry.getKey(), entry.getValue());
				this.cancelText = this.cancelText.replace(entry.getKey(), entry.getValue());
				this.acceptText = this.acceptText.replace(entry.getKey(), entry.getValue());
			}
			int f =0;
			
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
					randomElements.put(name, loadDictionary(dictFile));
				}
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
	}

	public TutorActor(Agent a)
	{
		super(a);
		introduction_cue_timeout = Integer.parseInt(properties.getProperty("timeout1"));
		introduction_cue_timeout2 = Integer.parseInt(properties.getProperty("timeout2"));
		request_poke_prompt_text = properties.getProperty("requestpokeprompt", request_poke_prompt_text);
		goahead_prompt_text = properties.getProperty("goaheadprompt", goahead_prompt_text);
		response_poke_prompt_text = properties.getProperty("responsepokeprompt",response_poke_prompt_text);
		dont_know_prompt_text = properties.getProperty("dontknowprompt", dont_know_prompt_text);
		moving_on_text = properties.getProperty("moveonprompt", moving_on_text);
		dialogueConfigFile = properties.getProperty("dialogue_config_file",dialogueConfigFile);
		dialogueFolder = properties.getProperty("dialogue_folder",dialogueFolder);
		startAnyways = properties.getProperty("start_anyways","false").equals("true");
		tutorialCondition = properties.getProperty("tutorial_condition",tutorialCondition);
		
		loadDialogConfiguration(dialogueConfigFile);
		
		prioritySource = new BlacklistSource("TUTOR_DIALOG", "");
		((BlacklistSource) prioritySource).addExceptions("TUTOR_DIALOG");
	}

	private void loadDialogConfiguration(String f)
	{
		try
		{
			DOMParser parser = new DOMParser();
			parser.parse(f);
			Document dom = parser.getDocument();
			NodeList dialogsNodes = dom.getElementsByTagName("dialogs");
			if ((dialogsNodes != null) && (dialogsNodes.getLength() != 0))
			{
				Element dialogsNode = (Element) dialogsNodes.item(0);
				NodeList dialogNodes = dialogsNode.getElementsByTagName("dialog");
				if ((dialogNodes != null) && (dialogNodes.getLength() != 0))
				{
					for (int i = 0; i < dialogNodes.getLength(); i++)
					{
						Element dialogElement = (Element) dialogNodes.item(i);
						String conceptName = dialogElement.getAttribute("concept");
						String name = dialogElement.getAttribute("scenario");
						String introText = null;
						String cueText = null;
						String cueAnnotation = null;
						String cancelText = "Dialog Abbruch durch Timeout";
						String cancelAnnotation = null;
						NodeList introNodes = dialogElement.getElementsByTagName("intro");
						if ((introNodes != null) && (introNodes.getLength() != 0))
						{
							Element introElement = (Element) introNodes.item(0);
							introText = introElement.getTextContent();
							/* Behzad, if you want to add hyperlink in dialog-config.xml, you need to do sth here!!!
							 * Right now, you don't know, but you will figure it out. Maybe this place is wrong at all.
							 * 
							 * IT IS A WRONG PLACE. USE HTML_TAGS IN DATA FOLDER of YOUR AGENT
							 */
							//introText = introText + " " + "<a href=\"https://www.google.com\">HERE.</a>";
						}
						NodeList cueNodes = dialogElement.getElementsByTagName("accept");
						if ((cueNodes != null) && (cueNodes.getLength() != 0))
						{
							Element cueElement = (Element) cueNodes.item(0);
							cueAnnotation = cueElement.getAttribute("annotation");
							cueText = cueElement.getTextContent();
						}

						NodeList cancelNodes = dialogElement.getElementsByTagName("cancel");
						if ((cancelNodes != null) && (cancelNodes.getLength() != 0))
						{
							Element cancelElement = (Element) cancelNodes.item(0);
							cancelAnnotation = cancelElement.getAttribute("annotation");
							cancelText = cancelElement.getTextContent();
						}
						/* Behzad: If you randomly select your dialog file HERE. It helps you to know your dialog before reading the knowledge_base file
						 *	If you know your entity here, you can prune the knowledge_base file or having several knowledge_based files.
						 *	BUT you did it here: look for "chooseDialogFileRandomly" in the current file.
						 */ 
						//name = chooseDialogFileRandomly();
						//-----------------------------------------
						
						Dialog d = new Dialog(conceptName, name, introText, cueAnnotation, cueText, cancelAnnotation, cancelText);
						
						
						proposedDialogs.put(conceptName, d);
					}
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected void processEvent(Event e)
	{
		/* Behzad: Based on the type of event, a function is called to handle it. I need a definition for each Event.
		 */
		if (e instanceof DoTutoringEvent)
		{
			handleDoTutoringEvent((DoTutoringEvent) e);
		}
		else if (e instanceof TutoringStartedEvent)
		{
			handleTutoringStartedEvent((TutoringStartedEvent) e);
		}
		else if (e instanceof PresenceEvent)
		{
			PresenceEvent event = (PresenceEvent) e;
			numUser = event.getNumUsers();
		}
		else if (e instanceof MessageEvent)
		{
			handleRequestDetectedEvent((MessageEvent) e);
		}
		else if (e instanceof StudentTurnsEvent)
		{
			handleStudentTurnsEvent((StudentTurnsEvent) e);
		}
		else if (e instanceof MoveOnEvent)
		{
			handleMoveOnEvent((MoveOnEvent) e);
		}
	}

	private void handleDoTutoringEvent(DoTutoringEvent dte)
	{
		Dialog d = proposedDialogs.get(dte.getConcept());
		if (d == null) {
			return;
		}
		
		if(isTutoring && !interruptForNewDialogues) {
			log(Logger.LOG_WARNING, "Won't start dialogue "+dte.getConcept()+" while current dialog is running - ask again later!");
			return;
		}
		
		if(isTutoring)
		{
			if (interruptForNewDialogues)
			{
				sendTutorMessage(moving_on_text);
				DoneTutoringEvent doneEvent = new DoneTutoringEvent(source, currentConcept, true);
				source.queueNewEvent(doneEvent);
				prioritySource.setBlocking(false);

				currentAutomata = null;
				currentConcept = null;
				isTutoring = false;
			}
		}
		
		launchDialogOffer(d, ((InputCoordinator)dte.getSender()).user);	
	}

	private synchronized void handleRequestDetectedEvent(MessageEvent e)
	{
		/* Behzad: Check here, this function check "accept" and "reject" annotations in dialogues-config
		 * maybe you find a way to choose a dialog by a prompt
		 */
		for(String concept : e.getAllAnnotations())
		{
			Dialog killMeNow = null;

			for(Dialog d : pendingDialogs.values())
			{

				if (d.acceptAnnotation.equals(concept))
				{

					killMeNow = d;
					sendTutorMessage(d.acceptText);
					startDialog(d);
				}
				else if(d.cancelAnnotation.equals(concept))
				{

					killMeNow = d;
					sendTutorMessage(d.cancelText);
					prioritySource.setBlocking(false);
					
				}
				if(killMeNow != null)
				{
					pendingDialogs.remove(killMeNow.acceptAnnotation);
					break;
				}
			}
		}
	}

	public void handleTutoringStartedEvent(TutoringStartedEvent tse)
	{
		/* Behzad
		 * Check here!!!
		 */
		if(currentConcept.equals(tse.getConcept()))
		{
			List<String> tutorTurns = currentAutomata.start();
			processTutorTurns(tutorTurns);
		}
		else
		{
			log(Logger.LOG_WARNING, "Received start event for "+tse+" but current concept is "+currentConcept);
		}
	}
		
	public List<String> getListOfScenarios(File dir) 
	{
		List<String> scenarios = new ArrayList<String>();
		File[] dictNames = dir.listFiles();

		for (File dictFile : dictNames)
		{
			if (dictFile.isDirectory())
				getListOfScenarios(dictFile);
			else if(dictFile.getName().endsWith(".xml") && dictFile.getName().startsWith("scenario"))
			{
				String name = dictFile.getName().replace(".xml", "");
				scenarios.add(name);
			}
		}		
		return scenarios;
	}
	
	
	public String chooseDialogFileRandomly()
	{
		// BEHZAD chooseDialogFileRandomly
		File folder = new File("dialogues/");
		List<String> scenario_files = getListOfScenarios(folder);
		Random rand = new Random();
	    String randomElement = scenario_files.get(rand.nextInt(scenario_files.size()));
	    source.dialog_name = randomElement;
	    return randomElement;

	}
	
	
	public void startDialog(Dialog d)
	{
		isTutoring = true;
		
		currentConcept = d.conceptName;
		currentAutomata = new TuTalkAutomata("tutor", "students");
		currentAutomata.setEvaluator(new FuzzyTurnEvaluator());
		
		
		/* 
		 * Behzad: By this part of code you can choose a dialog file randomly:
		*/
		d.scenarioName = chooseDialogFileRandomly();
		//-----------------------------------------------------------------------
		
		currentAutomata.setScenario(Scenario.loadScenario(dialogueFolder  + File.separator + d.scenarioName + ".xml"));
		
		TutoringStartedEvent tse = new TutoringStartedEvent(source, d.scenarioName, d.conceptName);
		source.queueNewEvent(tse);
	}

	private void handleMoveOnEvent(MoveOnEvent mve)
	{
		if (checkEventShouldBeHandled())
		{
			expectingResponse = false;
			noMatchingResponseCount = 0;
			List<Response> expected = currentAutomata.getState().getExpected();
			Response response = expected.get(expected.size() - 1); 
			
			if (!response.getConcept().getLabel().equalsIgnoreCase("unanticipated-response"))
			{
				log(Logger.LOG_ERROR, "Moving on without an Unanticipated-Response Handler. Could be weird!");
			}
			List<String> tutorTurns = currentAutomata.progress(response.getConcept());
			processTutorTurns(tutorTurns);
		}
	}

	private boolean checkEventShouldBeHandled() {
		return (expectingResponse && currentAutomata != null) ? true : false;
	}
	
	private boolean checkStudentEventShouldBeHandled(List<String> studentTurn) {
		return (checkEventShouldBeHandled() && !studentTurn.isEmpty()) ? true : false;
	}
	
	private synchronized void handleStudentTurnsEvent(StudentTurnsEvent ste)
	{
		if (checkStudentEventShouldBeHandled(ste.getStudentTurns()))
		{
			String studentTurn = "";
			for (String turn : ste.getStudentTurns())
			{
				studentTurn += turn + " | ";
			}
			List<EvaluatedConcept> matchingConcepts = currentAutomata.evaluateTuteeTurn(studentTurn, ste.getAnnotations());
			if (!matchingConcepts.isEmpty())
			{
				System.out.println(matchingConcepts.get(0).getClass().getSimpleName());
				System.out.println(matchingConcepts.get(0).concept.getClass().getSimpleName());
				Concept concept = matchingConcepts.get(0).concept;
				if (concept.getLabel().equalsIgnoreCase("unanticipated-response"))
				{
					if (!nullExpected)
					{
						noMatchingResponseCount++;
						
						if (matchingConcepts.size() == 1 || noMatchingResponseCount >= numUser + 2) {
							moveOn(concept);
						} else if (noMatchingResponseCount <= numUser + 2) {
							System.out.println("unanticipated");
						}
						return;
					}
				}
				else
				{
					for (String contributor : ste.getContributors())
					{
						answerers.add(contributor);
					}
				}
				
				moveOn(concept);
			}
		}
	}

	private void moveOn(Concept concept) {
		expectingResponse = false;
		noMatchingResponseCount = 0;
		List<String> tutorTurns = currentAutomata.progress(concept);
		processTutorTurns(tutorTurns);
	}
	
	private void messageAccepted()
	{
		if (currentAutomata != null)
		{
			List<Response> expected = currentAutomata.getState().getExpected();
			if (expected.size() != 0)
			{
				expectingResponse = true;
				nullExpected = false;
				noMatchingResponseCount = 0;

				if (expected.size() == 1)
				{
					if (expected.get(0).getConcept().getLabel().equalsIgnoreCase("unanticipated-response"))
					{
						nullExpected = true;
					}
				}
			}
			else
			{
				answerers = new ArrayList<String>();

				DoneTutoringEvent dte = new DoneTutoringEvent(source, currentConcept);
				prioritySource.setBlocking(false);
				source.queueNewEvent(dte);

				currentAutomata = null;
				currentConcept = null;
				isTutoring = false;
			}
		}
	}


	private void launchDialogOffer(Dialog d, String userName)
	{
		/* Behzad: In this function, one of greetings sentences will be selected.
		 * The list of greetings are located in /runtime/dialogues/dialogues-config.xml 
		 */
		prioritySource.setBlocking(true);
		sendTutorMessage(selectIntroString(d.introText, userName));
		pendingDialogs.put(d.acceptAnnotation, d);
		Timer t = new Timer(introduction_cue_timeout, d.conceptName, this);
		t.start();
	}
	
	private String selectIntroString(String intro, String userName) {
		// Behzad: randomly select one of the greeting sentences 
		String replacedText = null;
		if(userName != null) {
			replacedText = intro.replaceAll("\\[USERNAME\\]", userName);
		} else {
			replacedText = intro.replaceAll("\\[USERNAME\\]", "");
		}
		
		String[] texts = null;
		texts = replacedText.split(" \\| ");
		return texts[(int)(Math.random()*texts.length)];
	}
	
	//Behzad replaceRandomPhrase
	private List<String> replaceRandomPhrase(List<String> tutorTurns) {
		for (int i = 0; i < tutorTurns.size(); i++) {
			for (Map.Entry<String,String> entry : this.proposedDialogs.get(this.currentConcept).selectedRandomElement.entrySet()) {
				tutorTurns.set(i, tutorTurns.get(i).replace(entry.getKey(), entry.getValue()));
			}
			for (Map.Entry<String,String> entry : this.proposedDialogs.get(this.currentConcept).htmlTagElements.entrySet()) {
				tutorTurns.set(i, tutorTurns.get(i).replace(entry.getKey(), entry.getValue()));	
			}
			for (Map.Entry<String, List<String>> entry : this.proposedDialogs.get(this.currentConcept).knowledge_base.entrySet()) {
				String tmp = entry.getValue().get((int) Math.floor(entry.getValue().size() * Math.random()));
				tutorTurns.set(i, tutorTurns.get(i).replace(entry.getKey(), tmp));	
			}
		}
		
		
		return tutorTurns;
	}
	
	/* Behzad: When you want to use one dialog for all entities and also use USER_CLAIM_IS_POSITIVE in dialog file and finally write all dialog expression in knowledge_base.txt file
	 * 
	 * 	private List<String> replaceRandomPhrase(List<String> tutorTurns) {
		for (int i = 0; i < tutorTurns.size(); i++) {
			for (Map.Entry<String,String> entry : this.proposedDialogs.get(this.currentConcept).selectedRandomElement.entrySet()) {
				if (tutorTurns.get(i).equals("USER_CLAIM_IS_POSITIVE") && this.proposedDialogs.get(this.currentConcept).selectedRandomElement.get("INTELLIGENCE").equals("negative")) {
					tutorTurns.set(i, tutorTurns.get(i).replace("USER_CLAIM_IS_POSITIVE", this.proposedDialogs.get(this.currentConcept).selectedRandomElement.get("OPPOSITE_CLAIM")));
				}
				else if (tutorTurns.get(i).equals("USER_CLAIM_IS_NEGATIVE") && this.proposedDialogs.get(this.currentConcept).selectedRandomElement.get("INTELLIGENCE").equals("positive")) {
					tutorTurns.set(i, tutorTurns.get(i).replace("USER_CLAIM_IS_NEGATIVE", this.proposedDialogs.get(this.currentConcept).selectedRandomElement.get("OPPOSITE_CLAIM")));
				}
				else {
					tutorTurns.set(i, tutorTurns.get(i).replace(entry.getKey(), entry.getValue()));
				}
			}
			for (Map.Entry<String,String> entry : this.proposedDialogs.get(this.currentConcept).htmlTagElements.entrySet()) {
				tutorTurns.set(i, tutorTurns.get(i).replace(entry.getKey(), entry.getValue()));	
			}
		}
		
		
		return tutorTurns;
	}
	 * 
	 * 
	 */
	
	private void processTutorTurns(List<String> tutorTurns)
	{
		/* Behzad: Replace RANDOM_ELEMENT with a random phrase which is located into data/<conceptName>
		 * 
		 */
		tutorTurns = replaceRandomPhrase(tutorTurns);
		source.userMessages.handleToShortMessages(tutorTurns);
		String[] turns = tutorTurns.toArray(new String[0]);
		TutorTurnsEvent tte = new TutorTurnsEvent(source, turns);
		
		if(turns.length == 0) {
			return;
		}
		
		source.queueNewEvent(tte);
		PriorityEvent pete = new PriorityEvent(source, new MessageEvent(source, getAgent().getUsername(), join(turns), "TUTOR"), tutorMessagePriority, prioritySource, tutorTimeout);
		pete.addCallback(new Callback()
		{
			@Override
			public void rejected(PriorityEvent p)
			{
				log(Logger.LOG_ERROR, "Tutor Turn event was rejected. Proceeding anyway...");
				messageAccepted();
			}
			
			@Override
			public void accepted(PriorityEvent p)
			{
				messageAccepted();
			}
		});
		source.pushProposal(pete);
	}

	public void timedOut(String id)
	{
		Dialog d = null;
		if(proposedDialogs.get(id) == null)
		{
			log(Logger.LOG_WARNING, "No dialog for "+id);
		}
		else
		{
			d = pendingDialogs.get(proposedDialogs.get(id).acceptAnnotation);
		}
		
		log(Logger.LOG_NORMAL, "Timeout for "+id+":"+d);
		if (d != null)
		{
			sendTutorMessage(request_poke_prompt_text);
			Timer t = new Timer(introduction_cue_timeout2, "CANCEL:" + id, this);
			t.start();
			log(Logger.LOG_NORMAL, "Delaying dialog "+id+" once...");
		}
		else
		{
			if (isDialogTimedOutTwice(id))
			{
				killPendingDialog(id);
			}
		}
	}

	private boolean isDialogTimedOutTwice(String id) {
		return id.startsWith("CANCEL:") ? true : false; 
	}
	
	private void killPendingDialog(String id) {
		String[] tokens = id.split(":");
		Dialog dialog = proposedDialogs.get(tokens[1]);
		
		dialog = pendingDialogs.remove(dialog.acceptAnnotation);
		if (dialog != null)
		{
			if(startAnyways)
			{
				sendTutorMessage(goahead_prompt_text);

				log(Logger.LOG_NORMAL, "Not delaying "+tokens[1]+" anymore - beginning dialog");
				startDialog(dialog);
				return;
			}
			else
			{
				sendTutorMessage(dialog.cancelText);
			}
		}
		
		prioritySource.setBlocking(false);
	}
	
	private void sendTutorMessage(String... promptStrings)
	{
		String combo = join(promptStrings);
		
		if(!combo.equals("")) {
			PriorityEvent pete = new PriorityEvent(source, new MessageEvent(source, getAgent().getUsername(), combo, "TUTOR"), tutorMessagePriority , prioritySource, 45);
			source.pushProposal(pete);
		}
	}

	public String join(String... promptStrings)
	{
		String combo = "";
		for(String text : promptStrings)
		{
			combo += "|"+text;
		}
		return combo.substring(1);
	}

	public void log(String from, String level, String msg)
	{
		log(level, from + ": " + msg);
	}

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		this.source = source;
		processEvent(event);
		
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[]{MessageEvent.class, DoTutoringEvent.class,StudentTurnsEvent.class, MoveOnEvent.class, TutoringStartedEvent.class, PresenceEvent.class, PromptEvent.class};
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[0];
	}
}
