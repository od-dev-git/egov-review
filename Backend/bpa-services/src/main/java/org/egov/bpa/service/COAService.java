package org.egov.bpa.service;

import org.egov.bpa.web.model.COAModel;
import org.springframework.stereotype.Service;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Service
public class COAService {

	public COAModel fetchCOAData(String regNo) {

		try {
			// Search page
			Connection.Response response = Jsoup
					.connect("https://coa.gov.in/search_arch2.php?lang=1&level=1&linkid=&lid=289&searCat=3")
					.userAgent("Mozilla/5.0").timeout(30000).method(Connection.Method.GET).execute();

			Map<String, String> cookies = response.cookies();

			Document searchPage = response.parse();

			Element capCode = searchPage.selectFirst("input[name=cap_code]");

			if (capCode == null) {
				throw new RuntimeException("Captcha value not found.");
			}

			String captcha = capCode.val();

			// Search architect
			Document resultDoc = Jsoup
					.connect("https://coa.gov.in/search_architectResult.php?lang=1&level=1&linkid=&lid=289")
					.cookies(cookies).userAgent("Mozilla/5.0").timeout(30000).data("val_sub", "1").data("searCat", "3")
					.data("arc_reg", regNo).data("cap_code", captcha).data("T3", captcha).data("submit", "Search")
					.post();

			Element resultRow = resultDoc.select("table tbody tr.GridRow").first();

			if (resultRow == null) {
				throw new RuntimeException("No COA record found for Registration No : " + regNo);
			}

			Elements cols = resultRow.select("td");

			if (cols.size() < 7) {
				throw new RuntimeException("Unexpected COA response structure.");
			}

			COAModel coa = new COAModel();

			coa.setArchitectName(cols.get(1).text().trim());
			coa.setRegistrationNo(cols.get(2).text().trim());
			coa.setDisciplinary(cols.get(3).text().trim());

			// Full Address
			coa.setAddress(cols.get(4).text().trim());

			// Mobile
			coa.setMobile(cols.get(5).text().trim());

			// Email
			coa.setEmail(cols.get(6).text().trim());

			coa.setActive(true);

			return coa;

		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch COA data : " + ex.getMessage(), ex);
		}
	}
}
