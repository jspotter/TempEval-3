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
	 * Builds up annotation object with built in CoreNLP annotations as
	 * well as events.
	 */
	private static void annotate() throws Exception {
		
		// Read each training file in training directory
		File directory = new File(traindir);
		for (File child : directory.listFiles()) {
			if (child.getName().startsWith("."))
				continue;
			
			
			System.out.println("Training on fi8le " + child.getName());
			
			String file_text = "";
			String curr_line;
			BufferedReader br = new BufferedReader(new FileReader(child));
			
			while ((curr_line = br.readLine()) != null) {
					file_text += curr_line;
				}
		
			// Parse XML
			Document doc = XMLParser.parse(child);
		
			//NodeList e = doc.getElementsByTagName("TEXT");
			//Element ee = (Element) e.item(0);
		
			
			//Concealed Hacky way of getting training file content
			file_text = XMLParser.getRawTextByTagName(file_text, "<TEXT>", "</TEXT>");
	
			// Annotate with CoreNLP tags
			Annotation annotation = new Annotation(file_text);
			pipeline.annotate(annotation);
	
			// Annotate with events
			EventTagger.annotate(annotation);
			
			// Annotate with same-sentence event-timex pairs
			EventRelTagger.annotateEventTimex(annotation, doc);
			
			// Finally, add this annotation as a training example
			annotations.add(annotation);			
		}
		
		// Write annotations
		/*
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("sample.out"));
			for(Annotation a: annotations) {
				EventTagger.printAnnotations(a, out);
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
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
	}
}
