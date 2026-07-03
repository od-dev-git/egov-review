package org.egov.edcr.constants;

import java.util.HashMap;
import java.util.Map;

public class UlbNameConstants {
	private static final Map<String,String> map = new HashMap<String,String>();
	static {
		map.put("od.cuttack","Cuttack Municipal Corporation");
		map.put("od.banki","Banki");
		map.put("od.jatni","Jatni");
		map.put("od.angul","Angul");
		map.put("od.berhmpurMouza","Berhampur (Mouza Beyond Municipal Area)");
		map.put("od.bhubaneswarMouza","Bhubaneswar(Mouza Beyond Municipal Area)");
		map.put("od.biramitrapur","Biramitrapur");
		map.put("od.brhmapur","Berhampur");
		map.put("od.burla","Burla");
		map.put("od.chatrapur","Chatrapur");
		map.put("od.cuttackMouza","Cuttack (Mouza Beyond Municipal Area)");
		map.put("od.gopalpur","Gopalpur");
		map.put("od.hirakud","Hirakud");
		map.put("od.kalingaNagarMouza","Kalinga Nagar (Mouza Beyond Municipal Area)");
		map.put("od.khurda","Khurda");
		map.put("od.konark","Konark");
		map.put("od.paradip","Paradip");
		map.put("od.paradipMouza","Paradip (Mouza Beyond Municipal Area)");
		map.put("od.puri","Puri");
		map.put("od.puriMouza","Puri Konark (Mouza Beyond Municipal Area)");
		map.put("od.rajgangpur","Rajgangpur");
		map.put("od.rourkela","Rourkela");
		map.put("od.rourkelaMouza","Rourkela (Mouza Beyond Municipal Area)");
		map.put("od.sambalpur","Sambalpur");
		map.put("od.sambalpurMouza","Sambalpur (Mouza Beyond Municipal Area)");
		map.put("od.sundargarh","Sundargarh");
		map.put("od.talcher","Talcher");
		map.put("od.tamdaMouza","TAMDA (Mouza Beyond Municipal Area)");
		map.put("od.choudwar","Choudwar");
		
	}
	
	public static String ulbName(String tanantId ) {
		String name="Odisha Administration";
		if(map.get(tanantId)!=null)
			name=map.get(tanantId);
		return name;
	}
}
