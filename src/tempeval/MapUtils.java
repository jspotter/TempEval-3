package tempeval;

import java.util.*;

public class MapUtils {

	public static void doublePut(Map<String, Map<String, String>> map, 
			String key1, String key2, String value) {
		Map<String, String> valueMap;
		if (map.containsKey(key1)) {
			valueMap = map.get(key1);
		} else {
			valueMap = new HashMap<String, String>();
			map.put(key1, valueMap);
		}
		
		valueMap.put(key2, value);
	}
	
	public static String doubleGet(Map<String, Map<String, String>> map,
			String key1, String key2) {
		if (map.containsKey(key1))
			return map.get(key1).get(key2);
		else
			return null;
	}
	
}
