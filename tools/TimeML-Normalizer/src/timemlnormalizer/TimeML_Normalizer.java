package timemlnormalizer;

import java.io.*;
import java.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.*;
import nlp_files.XMLFile;
import utils_bk.*;
import xml_parsers.XmlAttribs;


public class TimeML_Normalizer {

    public static void normalize(ArrayList<File[]> annotations) {
        try {

            // use annotations[0] as key-guide
            File[] guide = annotations.get(0);


            // for each file
            for (int i = 0; i < guide.length; i++) {
                System.out.println("Normalizing " + guide[i].getName());
                HashMap<String, String>[] timex_map = new HashMap[annotations.size()];
                HashMap<String, String>[] event_map = new HashMap[annotations.size()];
                HashMap<String, String>[] mk_map = new HashMap[annotations.size()];
                ArrayList<String> xmlFileString = new ArrayList();
                ArrayList<String[]> tokenFileStringArr = new ArrayList();
                int last_eid = 1;
                int open_event = 0;
                int last_tid = 1;
                int[] open_timex = new int[annotations.size()];
                int[] last_tid_local = new int[annotations.size()];

                // for each annotation
                for (int a = 0; a < annotations.size(); a++) {
                    File annot = annotations.get(a)[i];
                    File ftdir = new File(annot.getCanonicalPath() + "-data");
                    if (ftdir.exists()) {
                        FileUtils.deleteRecursively(ftdir);
                    }
                    if (!ftdir.mkdirs()) {  // mkdirs creates many parent dirs if needed
                        throw new Exception("Directory not created...");
                    }
                    File workingfile = new File(ftdir + File.separator + annot.getName());
                    FileUtils.copyFileUtil(annot, workingfile);
                    XMLFile xmlfile = new XMLFile();
                    xmlfile.loadFile(workingfile);
                    String plainfile = xmlfile.toPlain(); // only <text>
                    String output = Tokenizer.run(plainfile);
                    output = merge_tok_n_tml(output, workingfile.getCanonicalPath());
                    tokenFileStringArr.add(FileUtils.readFileAsString(output, "UTF-8").split("\\n"));
                    xmlFileString.add(FileUtils.readFileAsString(workingfile.getCanonicalPath(), "UTF-8"));
                    event_map[a] = new HashMap<String, String>();
                    timex_map[a] = new HashMap<String, String>();
                    mk_map[a] = new HashMap<String, String>();
                    open_timex[a] = 0;
                    last_tid_local[a] = 0;
                }


                for (int linen = 0; linen < tokenFileStringArr.get(0).length; linen++) {
                    open_event = 0; // restart in every iteration (multi-token are not considered)
                    String last_token = null; // check all files are equal
                    for (int a = 0; a < annotations.size(); a++) {
                        String[] pipesarr = tokenFileStringArr.get(a)[linen].split("\\|");
                        if (last_token == null) {
                            last_token = pipesarr[0];
                        } else {
                            if (pipesarr[0].equals(last_token)) {
                                last_token = pipesarr[0];
                            } else {
                                throw new Exception("Tokens doesn't match at line: " + linen + " (" + last_token + ")");
                            }
                        }
                        HashMap<String, String> attribs = XmlAttribs.parseAttrs(pipesarr[2]);
                        if (pipesarr[1].matches("B-EVENT")) {
                            if (open_event == 0) {
                                open_event = last_eid;
                                last_eid++;
                            }
                            event_map[a].put(attribs.get("eid"), "e" + open_event);
                        }
                        // for timex maintain open_timex[] first
                        if (pipesarr[1].equals("O") || pipesarr[1].equals("B-TIMEX3")) {
                            open_timex[a] = 0; // restart
                        }
                    }

                    for (int a = 0; a < annotations.size(); a++) {
                        String[] pipesarr = tokenFileStringArr.get(a)[linen].split("\\|");
                        HashMap<String, String> attribs = XmlAttribs.parseAttrs(pipesarr[2]);
                        if (pipesarr[1].equals("B-TIMEX3")) {
                            open_timex[a] = last_tid; // new by default
                            // try to match with still open timexes
                            for (int at = 0; at < annotations.size(); at++) {
                                // open and not used in other annotations
                                if (a != at && open_timex[at] != 0 && last_tid_local[a] < open_timex[at]) {
                                    open_timex[a] = open_timex[at];
                                    break;
                                }
                            }
                            last_tid_local[a] = open_timex[a];
                            //new (not paired)
                            if (open_timex[a] == last_tid) {
                                last_tid++;
                            }
                            timex_map[a].put(attribs.get("tid"), "t" + open_timex[a]);
                        }
                        //if (pipesarr[1].matches("I-TIMEX3")) // just maintain opens open... do nothing.
                    }
                }

                // normalize entities given a map
                for (int a = 0; a < annotations.size(); a++) {
                    File ndir = new File(annotations.get(a)[i].getParent() + "-normalized");
                    if (!ndir.exists() && !ndir.mkdirs()) {  // mkdirs creates many parent dirs if needed
                        throw new Exception("Directory not created...");
                    }
                    String annotname = annotations.get(a)[i].getCanonicalPath().substring(0, annotations.get(a)[i].getCanonicalPath().lastIndexOf(File.separator));
                    annotname = annotname.substring(annotname.lastIndexOf(File.separator) + 1);
                    String tmp = xmlFileString.get(a);
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc = db.parse(annotations.get(a)[i]);
                    doc.getDocumentElement().normalize();
                    String dctid = null;
                    Element dct = ((Element) ((NodeList) ((Element) doc.getElementsByTagName("DCT").item(0)).getElementsByTagName("TIMEX3")).item(0));
                    if (dct != null) {
                        dctid = dct.getAttribute("tid");
                        timex_map[a].put(dctid, "t0");
                        // normalize to t0
                        tmp = tmp.replaceAll("(<DCT><TIMEX3[^>]*tid=\")" + dct.getAttribute("tid") + "(\"[^>]*>)", "$1t0$2");
                    }

                    NodeList text = doc.getElementsByTagName("TEXT");
                    if (text.getLength() > 1) {
                        throw new Exception("More than one TEXT tag found.");
                    }
                    Element TextElmnt = (Element) text.item(0); // If not ELEMENT NODE will throw exception


                    // normalize events (entity by entity in order) ---- // deprecated solution: e1=e3 -- e3=e1 map problem. //for(String e:event_map[a].keySet()){tmp=tmp.replaceAll("(eid|eventID)=\""+e+"\"", event_map[a].get(e));}
                    NodeList current_node = TextElmnt.getElementsByTagName("EVENT");
                    for (int s = 0; s < current_node.getLength(); s++) {
                        Element element = (Element) current_node.item(s);
                        tmp = tmp.replaceAll("(<EVENT[^>]*eid=\")" + element.getAttribute("eid") + "(\"[^>]*>)", "$1n" + event_map[a].get(element.getAttribute("eid")) + "$2");
                    }
                    tmp = tmp.replaceAll("(<EVENT[^>]*eid=\")n", "$1");

                    // normalize timexes
                    current_node = TextElmnt.getElementsByTagName("TIMEX3");
                    for (int s = 0; s < current_node.getLength(); s++) {
                        Element element = (Element) current_node.item(s);
                        if(timex_map[a].get(element.getAttribute("tid"))!=null){
                            tmp = tmp.replaceAll("(<TIMEX3[^>]*tid=\")" + element.getAttribute("tid") + "(\"[^>]*>)", "$1n" + timex_map[a].get(element.getAttribute("tid")) + "$2");
                            if (element.hasAttribute("anchorTimeID")) {
                                if(timex_map[a].get(element.getAttribute("anchorTimeID"))!=null){
                                    tmp = tmp.replaceAll("(<TIMEX3[^>]*anchorTimeID=\")" + element.getAttribute("anchorTimeID") + "(\"[^>]*>)", "$1n" + timex_map[a].get(element.getAttribute("anchorTimeID")) + "$2");
                                }else{
                                    tmp = tmp.replaceAll("anchorTimeID=\"" + element.getAttribute("anchorTimeID") + "\"", "");
                                }
                            }
                            if (element.hasAttribute("beginPoint")) {
                                if(timex_map[a].get(element.getAttribute("beginPoint"))!=null){
                                    tmp = tmp.replaceAll("(<TIMEX3[^>]*beginPoint=\")" + element.getAttribute("beginPoint") + "(\"[^>]*>)", "$1n" + timex_map[a].get(element.getAttribute("beginPoint")) + "$2");
                                }else{
                                    tmp = tmp.replaceAll("beginPoint=\"" + element.getAttribute("beginPoint") + "\"", "");                                    
                                }
                            }
                            if (element.hasAttribute("endPoint")) {
                                if(timex_map[a].get(element.getAttribute("endPoint"))!=null){
                                    tmp = tmp.replaceAll("(<TIMEX3[^>]*endPoint=\")" + element.getAttribute("endPoint") + "(\"[^>]*>)", "$1n" + timex_map[a].get(element.getAttribute("endPoint")) + "$2");
                                }else{
                                    tmp = tmp.replaceAll("endPoint=\"" + element.getAttribute("endPoint") + "\"", "");
                                }
                            }
                        }else{
                            tmp = tmp.replaceAll("<TIMEX3[^>]*tid=\"" + element.getAttribute("tid") + "\"[^>]*>([^<]*)</TIMEX3>", "$1");
                            tmp = tmp.replaceAll("anchorTimeID=\"" + element.getAttribute("tid") + "\"", "");
                            tmp = tmp.replaceAll("beginPoint=\"" + element.getAttribute("tid") + "\"", "");
                            tmp = tmp.replaceAll("endPoint=\"" + element.getAttribute("tid") + "\"", "");
                            tmp = tmp.replaceAll("<[TSA]LINK[^>]*=\"" + element.getAttribute("tid") + "\"[^>]*>", "");
                        }
                    }
                    tmp = tmp.replaceAll("(<TIMEX3[^>]*tid=\")n", "$1");
                    tmp = tmp.replaceAll("(<TIMEX3[^>]*anchorTimeID=\")n", "$1");
                    tmp = tmp.replaceAll("(<TIMEX3[^>]*beginPoint=\")n", "$1");
                    tmp = tmp.replaceAll("(<TIMEX3[^>]*endPoint=\")n", "$1");

                    // normalize and map makeinstances
                    HashMap<String, ArrayList<String>> event_mk_index = new HashMap<String, ArrayList<String>>();
                    HashSet<String> mks_new_ids = new HashSet<String>();
                    current_node = null;
                    current_node = doc.getElementsByTagName("MAKEINSTANCE");
                    for (int s = 0; s < current_node.getLength(); s++) {
                        Element element = (Element) current_node.item(s);
                        String mapped_event = event_map[a].get(element.getAttribute("eventID"));
                        if (!event_mk_index.containsKey(mapped_event)) {
                            ArrayList<String> mks = new ArrayList<String>();
                            if (mks_new_ids.contains(mapped_event.replaceFirst("e", "ei"))) {
                                throw new Exception("Duplicated eiid in mks: " + mapped_event.replaceFirst("e", "ei") + "  " + annotations.get(a)[i]);
                            }
                            mks_new_ids.add(mapped_event.replaceFirst("e", "ei"));
                            mks.add(mapped_event.replaceFirst("e", "ei"));
                            event_mk_index.put(mapped_event, mks);
                            mk_map[a].put(element.getAttribute("eiid"), mapped_event.replaceFirst("e", "ei"));
                            tmp = tmp.replaceAll("(<MAKEINSTANCE[^>]*eiid=\"" + element.getAttribute("eiid") + "\"[^>]*[^>]*eventID=\")" + element.getAttribute("eventID") + "(\"[^>]*>)", "$1n" + mapped_event + "$2");
                            tmp = tmp.replaceAll("(<MAKEINSTANCE[^>]*[^>]*eventID=\")" + element.getAttribute("eventID") + "(\"[^>]*eiid=\"" + element.getAttribute("eiid") + "\"[^>]*>)", "$1n" + mapped_event + "$2");
                            tmp = tmp.replaceAll("(<MAKEINSTANCE[^>]*eiid=\")" + element.getAttribute("eiid") + "(\"[^>]*>)", "$1n" + mapped_event.replaceFirst("e", "ei") + "$2");
                        } else {
                            if (mks_new_ids.contains(mapped_event.replaceFirst("e", "ei"))) {
                                int num = 1000000 + Integer.parseInt(mapped_event.substring(1));
                                while (mks_new_ids.contains("ei" + num)) {
                                    num++;
                                }
                                mks_new_ids.add("ei" + num);
                                event_mk_index.get(mapped_event).add("ei" + num);
                                mk_map[a].put(element.getAttribute("eiid"), "ei" + num);
                                tmp = tmp.replaceAll("(<MAKEINSTANCE[^>]*eiid=\"" + element.getAttribute("eiid") + "\"[^>]*[^>]*eventID=\")" + element.getAttribute("eventID") + "(\"[^>]*>)", "$1n" + mapped_event + "$2");
                                tmp = tmp.replaceAll("(<MAKEINSTANCE[^>]*[^>]*eventID=\")" + element.getAttribute("eventID") + "(\"[^>]*eiid=\"" + element.getAttribute("eiid") + "\"[^>]*>)", "$1n" + mapped_event + "$2");
                                tmp = tmp.replaceAll("(<MAKEINSTANCE[^>]*eiid=\")" + element.getAttribute("eiid") + "(\"[^>]*>)", "$1n" + "ei" + num + "$2");
                            } else {
                                throw new Exception("Extrange");
                            }
                        }
                    }
                    tmp = tmp.replaceAll("(<MAKEINSTANCE[^>]*eiid=\")n", "$1");
                    tmp = tmp.replaceAll("(<MAKEINSTANCE[^>]*eventID=\")n", "$1");



                    // normalize links
                    current_node = doc.getElementsByTagName("TLINK");
                    for (int s = 0; s < current_node.getLength(); s++) {
                        Element element = (Element) current_node.item(s);
                        String relType = element.getAttribute("relType");
                        if (relType.matches("(DURING|DURING_INV|IDENTITY)")) {
                            relType = "SIMULTANEOUS";
                        }
                        String entity1 = null;
                        String entity2 = null;

                        // event-event
                        if (element.hasAttribute("eventInstanceID") && element.hasAttribute("relatedToEventInstance")) {
                            if (!mk_map[a].containsKey(element.getAttribute("eventInstanceID"))) {
                                throw new Exception("Event instance not found it file: " + element.getAttribute("eventInstanceID"));
                            }
                            if (!mk_map[a].containsKey(element.getAttribute("relatedToEventInstance"))) {
                                throw new Exception("Event instance not found it file: " + element.getAttribute("relatedToEventInstance"));
                            }

                            entity1 = mk_map[a].get(element.getAttribute("eventInstanceID"));
                            entity2 = mk_map[a].get(element.getAttribute("relatedToEventInstance"));
                            // Order by id (for normalization)
                            if (Integer.parseInt(entity1.substring(2)) > Integer.parseInt(entity2.substring(2))) {
                                entity1 = entity2;
                                entity2 = mk_map[a].get(element.getAttribute("eventInstanceID"));
                                relType = reverseRelationCategory(relType);
                            }
                            tmp = tmp.replaceAll("(<TLINK[^>]*lid=\"" + element.getAttribute("lid") + "\"[^>]*[^>]*eventInstanceID=\")" + element.getAttribute("eventInstanceID") + "(\"[^>]*>)", "$1" + entity1 + "$2");
                            tmp = tmp.replaceAll("(<TLINK[^>]*lid=\"" + element.getAttribute("lid") + "\"[^>]*[^>]*relatedToEventInstance=\")" + element.getAttribute("relatedToEventInstance") + "(\"[^>]*>)", "$1" + entity2 + "$2");
                            tmp = tmp.replaceAll("(<TLINK[^>]*lid=\"" + element.getAttribute("lid") + "\"[^>]*[^>]*relType=\")" + element.getAttribute("relType") + "(\"[^>]*>)", "$1" + relType + "$2");
                        }
                        // event-time
                        if (element.hasAttribute("eventInstanceID") && element.hasAttribute("relatedToTime")) {
                            if (!mk_map[a].containsKey(element.getAttribute("eventInstanceID"))) {
                                throw new Exception("Event instance not found in file: " + element.getAttribute("eventInstanceID"));
                            }
                            if (!timex_map[a].containsKey(element.getAttribute("relatedToTime"))) {
                                throw new Exception("Timex not found in file: " + element.getAttribute("relatedToTime"));
                            }

                            entity1 = mk_map[a].get(element.getAttribute("eventInstanceID"));
                            entity2 = timex_map[a].get(element.getAttribute("relatedToTime"));
                            tmp = tmp.replaceAll("(<TLINK[^>]*lid=\"" + element.getAttribute("lid") + "\"[^>]*[^>]*eventInstanceID=\")" + element.getAttribute("eventInstanceID") + "(\"[^>]*>)", "$1" + entity1 + "$2");
                            tmp = tmp.replaceAll("(<TLINK[^>]*lid=\"" + element.getAttribute("lid") + "\"[^>]*[^>]*relatedToTime=\")" + element.getAttribute("relatedToTime") + "(\"[^>]*>)", "$1" + entity2 + "$2");
                        }
                        if (element.hasAttribute("timeID") && element.hasAttribute("relatedToEventInstance")) {
                            if (!mk_map[a].containsKey(element.getAttribute("relatedToEventInstance"))) {
                                throw new Exception("Event instance not found it file: " + element.getAttribute("relatedToEventInstance"));
                            }
                            if (!timex_map[a].containsKey(element.getAttribute("timeID"))) {
                                throw new Exception("Timex not found it file: " + element.getAttribute("timeID"));
                            }
                            entity1 = mk_map[a].get(element.getAttribute("relatedToEventInstance"));
                            entity2 = timex_map[a].get(element.getAttribute("timeID"));
                            relType = reverseRelationCategory(relType);
                            tmp = tmp.replaceAll("(<TLINK[^>]*lid=\"" + element.getAttribute("lid") + "\"[^>]*[^>]*)timeID=\"" + element.getAttribute("timeID") + "(\"[^>]*>)", "$1eventInstanceID=\"" + entity1 + "$2");
                            tmp = tmp.replaceAll("(<TLINK[^>]*lid=\"" + element.getAttribute("lid") + "\"[^>]*[^>]*)relatedToEventInstance=\"" + element.getAttribute("relatedToEventInstance") + "(\"[^>]*>)", "$1relatedToTime=\"" + entity2 + "$2");
                            tmp = tmp.replaceAll("(<TLINK[^>]*lid=\"" + element.getAttribute("lid") + "\"[^>]*[^>]*relType=\")" + element.getAttribute("relType") + "(\"[^>]*>)", "$1" + relType + "$2");
                        }
                        // time-time
                        if (element.hasAttribute("timeID") && element.hasAttribute("relatedToTime")) {
                            if (!timex_map[a].containsKey(element.getAttribute("relatedToTime"))) {
                                throw new Exception("Timex instance not found it file: " + element.getAttribute("relatedToTime"));
                            }
                            if (!timex_map[a].containsKey(element.getAttribute("timeID"))) {
                                throw new Exception("Timex not found it file: " + element.getAttribute("timeID") + " - "+element.getAttribute("lid"));
                            }
                            entity1 = timex_map[a].get(element.getAttribute("timeID"));
                            entity2 = timex_map[a].get(element.getAttribute("relatedToTime"));
                            // Order by id (for normalization)
                            if (Integer.parseInt(entity1.substring(1)) > Integer.parseInt(entity2.substring(1))) {
                                entity1 = entity2;
                                entity2 = timex_map[a].get(element.getAttribute("timeID"));
                                relType = reverseRelationCategory(relType);
                            }
                            tmp = tmp.replaceAll("(<TLINK[^>]*lid=\"" + element.getAttribute("lid") + "\"[^>]*[^>]*timeID=\")" + element.getAttribute("timeID") + "(\"[^>]*>)", "$1" + entity1 + "$2");
                            tmp = tmp.replaceAll("(<TLINK[^>]*lid=\"" + element.getAttribute("lid") + "\"[^>]*[^>]*relatedToTime=\")" + element.getAttribute("relatedToTime") + "(\"[^>]*>)", "$1" + entity2 + "$2");
                            tmp = tmp.replaceAll("(<TLINK[^>]*lid=\"" + element.getAttribute("lid") + "\"[^>]*[^>]*relType=\")" + element.getAttribute("relType") + "(\"[^>]*>)", "$1" + relType + "$2");
                        }

                    }
                    // For safety ALINKS and SLINKs are removed, also rlinks
                    tmp = tmp.replaceAll("<[ASR]LINK.*", "");


                    // save normalized annotation
                    FileUtils.writeFileFromString(tmp, ndir + File.separator + annotations.get(a)[i].getName());
                    File annot = annotations.get(a)[i];
                    File ftdir = new File(annot.getCanonicalPath() + "-data");
                    if (ftdir.exists()) {
                        FileUtils.deleteRecursively(ftdir);
                    }
                }

            }

        } catch (Exception e) {
            System.err.println("Errors found (TimeML_Normalizer):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }

    }

    public static String merge_tok_n_tml(String tokfile, String tmlfile) {
        String outputfile = tokfile + "-IOB2";

        try {
            BufferedWriter outfile = new BufferedWriter(new FileWriter(outputfile));
            boolean hasRoot_tag = false;
            char cxml = '\0';
            String line;
            String tag = "", attribs = "-", inTag = "", inAttribs = "-";
            //boolean closingtag = false;
            char BIO = 'O';

            BufferedReader xmlreader = new BufferedReader(new FileReader(tmlfile));
            BufferedReader pipesreader = new BufferedReader(new FileReader(tokfile));
            try {
                // find root tag
                while (true) {
                    if ((cxml = (char) xmlreader.read()) == -1) {
                        throw new Exception("Premature end of model file");
                    }
                    if (cxml == '<') {
                        if ((cxml = (char) xmlreader.read()) == -1) {
                            throw new Exception("Premature end of model file");
                        }
                        do {
                            tag += cxml;
                            if ((cxml = (char) xmlreader.read()) == -1) {
                                throw new Exception("Premature end of model file");
                            }
                        } while (cxml != '>');
                        if (tag.equalsIgnoreCase("TEXT")) {
                            hasRoot_tag = true;
                            break;
                        }
                        tag = "";

                    }
                    //System.err.print(cxml);
                }
                if (!hasRoot_tag) {
                    throw new Exception("Root tag " + "TEXT" + " not found");
                }

                tag = "";
                cxml = '\0';

                while ((line = pipesreader.readLine()) != null) {
                    if (line.length() < 1) {
                        throw new Exception("Malformed tokens file: empty line.");
                    }
                    boolean interTokenTag = false;
                    boolean findtokenIter = false;
                    boolean delayed_closing = false;
                    char prevxmlchar = 'x';
                    for (int cn = 0; cn < line.length(); cn++) {
                        char cpipes = line.charAt(cn);
                        prevxmlchar = cxml;
                        if ((cxml = (char) xmlreader.read()) == -1) {
                            throw new Exception("Premature end of model file");
                        }
                        //System.err.println("cxml(" + cxml + ") cpipes(" + cpipes + "," + cn + ") " + inTag);
                        if (Character.toLowerCase(cpipes) != Character.toLowerCase(cxml)) {
                            if (cxml == ' ' || cxml == '\n' || cxml == '\r' || cxml == '\t') {
                                cn--;
                                //System.err.println("blank found cn=" + cn);
                            } else {
                                // tags handling
                                if (cxml == '<') {
                                    if (cn != 0) {
                                        interTokenTag = true;
                                    }
                                    cn--;
                                    while (((cxml = (char) xmlreader.read()) != (char) -1) && cxml != '>') {
                                        tag += cxml;
                                    }
                                    tag = tag.trim();
                                    if (tag.indexOf(' ') != -1) {
                                        attribs = tag.substring(tag.indexOf(' ') + 1);
                                        tag = tag.substring(0, tag.indexOf(' '));
                                    }
                                    //System.err.println("tag=" + tag + " attribs=" + attribs);
                                    if (tag.matches("(?i)(EVENT|TIMEX3)")) {
                                        findtokenIter = true;
                                        //System.err.println("LOOKING opening tag=" + tag + " attribs=" + attribs);
                                        if (interTokenTag) {
                                            System.err.println("Inter-token (" + cn + ") tag consider manual tokenizing: " + line);
                                        }
                                        if (!inTag.equals("")) {
                                            throw new Exception("Nested tags (" + tag + "/" + inTag + ") consider manual correction: " + line);
                                        }
                                        inTag = tag;
                                        inAttribs = attribs;
                                        tag = "";
                                        attribs = "-";
                                        BIO = 'B';
                                    } else {
                                        interTokenTag = false;
                                    }
                                    // check if closing
                                    if (tag.matches("/.*")) {
                                        String check = inTag;
                                        if (tag.matches("/" + "(?i)" + check)) {
                                            if (findtokenIter) {
                                                // safe for empty tags (events_4_instances and timex3_4_durations)
                                                if (cn >= 0) {
                                                    System.err.println("Inter Token end of tag (" + inTag + ") cn=" + cn + " " + line);
                                                    delayed_closing = true;
                                                } else {
                                                    BIO = 'O';
                                                    inTag = "";
                                                    inAttribs = "-";
                                                    findtokenIter = false;
                                                    interTokenTag = false;
                                                }
                                            } else {
                                                //System.err.println("closing tag=" + inTag);
                                                BIO = 'O';
                                                inTag = "";
                                            }
                                        }
                                    }
                                    // check if end "TEXT"
                                    if (tag.matches("/" + "(?i)" + "TEXT")) {
                                        System.err.println("closing TEXT=" + "TEXT");
                                        // do something
                                        // it never reaches this because tok file ends before.
                                    }
                                    tag = "";
                                    attribs = "-";
                                } else {
                                    // escaped & < >
                                    if (cxml == '&' || (prevxmlchar == '&' && cxml == 'a')) {
                                        cn--;
                                        while (((cxml = (char) xmlreader.read()) != (char) -1) && cxml != ';') {
                                            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                                                System.err.println("Reading XML escaped char in: " + line);
                                            }
                                        }
                                    } else {
                                        throw new Exception("Distinct chars cxml(" + cxml + ") cpipes(" + cpipes + ")");
                                    }
                                }
                            }
                        }
                    }
                    outfile.write(line + "|" + BIO);
                    if (BIO != 'O') {
                        outfile.write("-" + inTag);
                    }
                    outfile.write("|" + inAttribs + "\n");
                    if (BIO == 'B') {
                        BIO = 'I';
                        inAttribs = "-";
                    }
                    if (delayed_closing) {
                        BIO = 'O';
                        inTag = "";
                        inAttribs = "-";
                        findtokenIter = false;
                        interTokenTag = false;
                        delayed_closing = false;
                    }
                }
            } finally {
                if (pipesreader != null) {
                    pipesreader.close();
                }
                if (xmlreader != null) {
                    xmlreader.close();
                }
                if (outfile != null) {
                    outfile.close();
                }
            }
        } catch (Exception e) {
            System.err.println("Errors found (TimeML_Normalizer):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return null;
        }
        return outputfile;
    }

    /**
     * Given a relation name, return the inverse Allen-TimeML relation
     * @param rel
     * @return
     */
    public static String reverseRelationCategory(String rel) {
        try {
            if (rel.equals("BEFORE")) {
                return "AFTER";
            }
            if (rel.equals("AFTER")) {
                return "BEFORE";
            }
            if (rel.equals("IBEFORE")) {
                return "IAFTER";
            }
            if (rel.equals("IAFTER")) {
                return "IBEFORE";
            }
            if (rel.equals("DURING")) {
                return "DURING_INV";
            }
            if (rel.equals("BEGINS")) {
                return "BEGUN_BY";
            }
            if (rel.equals("BEGUN_BY")) {
                return "BEGINS";
            }
            if (rel.equals("ENDS")) {
                return "ENDED_BY";
            }
            if (rel.equals("ENDED_BY")) {
                return "ENDS";
            }
            if (rel.equals("OVERLAPS")) {
                return "OVERLAPPED_BY";
            }
            if (rel.equals("OVERLAPPED_BY")) {
                return "OVERLAPS";
            }
            if (rel.equals("INCLUDES")) {
                return "IS_INCLUDED";
            }
            if (rel.equals("IS_INCLUDED")) {
                return "INCLUDES";
            }
            if (rel.equals("IDENTITY") || rel.equals("SIMULTANEOUS")) {
                return "SIMULTANEOUS";
            }
            throw new Exception("Unknow relation: " + rel);
        } catch (Exception e) {
            System.err.println("Errors found (TimeML_Normalizer):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return null;
        }
    }
}
