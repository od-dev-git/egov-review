package org.egov.edcr.od;

public class FloorNumberToWord {
	public static final String FLOOR = "Floor";

	public static String floorName(Integer floorNumber, boolean isStiltFloor, boolean isServiceFloor) {
		String result = "";
		if (isStiltFloor) {
			result = "Stilt Floor";
			if(floorNumber>0)
				result = result+" Level "+numberToWord(String.valueOf(floorNumber));
			return result;
		}
		if (isServiceFloor) {
			result = "Service floor";
			if(floorNumber>0)
				result = result+" Level "+numberToWord(String.valueOf(floorNumber));
			return result;
		}
		if (floorNumber < 0) {
			result = "Basement level " + numberToWord(String.valueOf(floorNumber * -1));
		} else if (floorNumber == 0) {
			result = "Ground floor";
		} else {
			result = numberToWord(String.valueOf(floorNumber)) + " " + FLOOR;
		}
		return result;
	}

	private static String numberToWord(String number) {
		String twodigitword = "";
		String word = "";
		String[] HTLC = { "", "Hundred", "Thousand", "Lakh", "Crore" }; // H-hundread , T-Thousand, ..
		int split[] = { 0, 2, 3, 5, 7, 9 };
		String[] temp = new String[split.length];
		boolean addzero = true;
		int len1 = number.length();
		if (len1 > split[split.length - 1]) {
			//System.out.println("Error. Maximum Allowed digits " + split[split.length - 1]);
			System.exit(0);
		}
		for (int l = 1; l < split.length; l++)
			if (number.length() == split[l])
				addzero = false;
		if (addzero == true)
			number = "0" + number;
		int len = number.length();
		int j = 0;
		// spliting & putting numbers in temp array.
		while (split[j] < len) {
			int beg = len - split[j + 1];
			int end = beg + split[j + 1] - split[j];
			temp[j] = number.substring(beg, end);
			j = j + 1;
		}

		for (int k = 0; k < j; k++) {
			twodigitword = ConvertOnesTwos(temp[k]);
			if (k >= 1) {
				if (twodigitword.trim().length() != 0)
					word = twodigitword + " " + HTLC[k] + " " + word;
			} else
				word = twodigitword;
		}
		return (word);
	}

	private static String ConvertOnesTwos(String t) {
		final String[] ones = { "", "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth", "Tenth",
				"Eleventh", "twelfth", "Thirteenth", "Fourteenth", "Fifteenth", "Sixteenth", "Seventeenth", "Eighteenth", "Nineteenth" };
		final String[] tens = { "", "Tenth", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty",
				"Ninety" };

		String word = "";
		int num = Integer.parseInt(t);
		if (num % 10 == 0)
			word = tens[num / 10] + " " + word;
		else if (num < 20)
			word = ones[num] + " " + word;
		else {
			word = tens[(num - (num % 10)) / 10] + word;
			word = word + " " + ones[num % 10];
		}
		return word;
	}
}