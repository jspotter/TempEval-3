package tempeval;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;


import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

public class Runner {

	private static final String traindir = 
			"data/TBAQ-cleaned/AQUAINT";

	private static StanfordCoreNLP pipeline;
	private static ArrayList<Annotation> annotations;

	/*
	 * Gets training text. This is everything in the <TEXT> tag INCLUDING
	 * EVENT, TIMEX, and SIGNAL tags themselves.
	 */
	private static String getTrainingText(File child) throws IOException {
		String fileText = "";
		String currLine;
		BufferedReader br = new BufferedReader(new FileReader(child));

		while ((currLine = br.readLine()) != null) {
			fileText += currLine;
		}

		//NodeList e = doc.getElementsByTagName("TEXT");
		//Element ee = (Element) e.item(0);


		//Concealed Hacky way of getting training file content
		fileText = XMLParser.getRawTextByTagName(fileText, "<TEXT>", "</TEXT>");
		return fileText;
	}

	/*
	 * Gets test text. This is everything in the <TEXT> tag EXCLUDING
	 * EVENT, TIMEX, and SIGNAL tags themselves.
	 */
	private static String getTestingText(Document doc) {
		return XMLParser.getElementTextByTagNameNR(doc.getDocumentElement(), "TEXT");
	}

	/*
	 * Builds up annotation object with built in CoreNLP annotations as
	 * well as events.
	 */
	private static void annotate() throws Exception {

		TimexEventTagger.initTagger();

		// Read each training file in training directory
		int filesRead = 0;
		File directory = new File(traindir);
		for (File child : directory.listFiles()) {
			if (child.getName().startsWith("."))
				continue;
			
			// Parse XML
			Document doc = XMLParser.parse(child);
			
			Annotation annotation = new Annotation(getTrainingText(child));
			pipeline.annotate(annotation);

			// Annotate with events
			EventTagger.annotate(annotation, doc);

			// Annotate with same-sentence event-timex pairs
			TimexEventTagger.trainEventTimex(annotation, doc);

			// Finally, add this annotation as a training example
			annotations.add(annotation);

			// Uncomment to test just ten files
			if (++filesRead >= 10) break;
		}

		TimexEventTagger.doneClassifying();
		
		EventTagger.loadTestClassifier();
		TimexEventTagger.loadTestClassifier();

		// Test
		for (File child : directory.listFiles()) {
			if (child.getName().startsWith("."))
				continue;
			
			// Parse XML
			Document doc = XMLParser.parse(child);

			Annotation annotation = new Annotation(getTestingText(doc));
			pipeline.annotate(annotation);

			// Annotate with events
			EventTagger.testEventTagger(annotations);

			// Annotate with same-sentence event-timex pairs
			TimexEventTagger.testEventTimex(annotation, doc);
		}

		TimexEventTagger.doneTesting();

		// Write annotations
		/*try {
			BufferedWriter out = new BufferedWriter(new FileWriter("sample.out"));
			for(Annotation a: annotations) {
				EventTagger.printEventAnnotations(a, out);
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {

		// Create pipeline
		Properties props = new Properties();
		//props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		props.put("annotators", "tokenize, ssplit");
		pipeline = new StanfordCoreNLP(props);
		annotations = new ArrayList<Annotation>();

		// Get XML file
		//Document trainDoc = XMLParser.parse(trainfile);
		//Document testDoc = XMLParser.parse(testfile);

		// Get text of XML file
		//Element root = trainDoc.getDocumentElement();
		//Element[] texts = XMLParser.getElementsByTagNameNR(root, "TEXT");
		//Element text = texts[0];

		try {
			annotate();
		} catch (Exception e) {
			e.printStackTrace();
		}

		/*
		String CRFClassifierFilePath = "classifiers/event-model.ser.gz";
		EventTagger.testEventTagger(annotations, CRFClassifierFilePath);
		 */
	}
}
