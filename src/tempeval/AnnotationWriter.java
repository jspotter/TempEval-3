package tempeval;

import java.io.BufferedWriter;
import java.io.IOException;

import edu.stanford.nlp.pipeline.Annotation;

public class AnnotationWriter {

	public static final String HEADER = "<?xml version=\"1.0\" ?>\n"
			+ "<TimeML xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
			+ "xsi:noNamespaceSchemaLocation=\"http://timeml.org/timeMLdocs/TimeML_1.2.1.xsd\">";
	public static final String FOOTER = "</TimeML>";
	
	public static void writeAnnotation(Annotation annotation, BufferedWriter out) 
			throws IOException {
		
	}
}
