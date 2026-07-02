package org.egov.collection.util;

import static java.util.Objects.isNull;

import java.math.BigDecimal;
import java.util.Iterator;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Utils {

    private Utils(){}

    public static JsonNode jsonMerge(JsonNode mainNode, JsonNode updateNode) {

        if(isNull(mainNode) || mainNode.isNull())
            return updateNode;
        if (isNull(updateNode) || updateNode.isNull())
            return mainNode;

        Iterator<String> fieldNames = updateNode.fieldNames();
        while (fieldNames.hasNext()) {

            String fieldName = fieldNames.next();
            JsonNode jsonNode = mainNode.get(fieldName);
            // if field exists and is an embedded object
            if (jsonNode != null && jsonNode.isObject()) {
                jsonMerge(jsonNode, updateNode.get(fieldName));
            }
            else {
                if (mainNode instanceof ObjectNode) {
                    // Overwrite field
                    JsonNode value = updateNode.get(fieldName);
                    ((ObjectNode) mainNode).put(fieldName, value);
                }
            }

        }

        return mainNode;
    }
    
	public static boolean isPositiveInteger(BigDecimal bd) {
		return bd.compareTo(BigDecimal.ZERO) >= 0
				&& (bd.signum() == 0 || bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0);
	}
	

    public static Long getStartingDateOfCurrentFy() {
        // Get the current date
        LocalDate currentDate = LocalDate.now();

        // Determine the starting month and year of the financial year
        int financialYearStartMonth = 4; // Assuming April as the starting month
        int currentYear = currentDate.getYear();
        int financialYearStartYear = (currentDate.getMonthValue() < financialYearStartMonth) ? currentYear - 1 : currentYear;

        // Create a LocalDate object for the starting date of the financial year
        LocalDate financialYearStartDate = YearMonth.of(financialYearStartYear, financialYearStartMonth).atDay(1);

        // Convert the LocalDate to epoch time in milliseconds with GMT timezone
        long epochTime = financialYearStartDate.atStartOfDay(ZoneId.of("GMT")).toInstant().toEpochMilli();

        log.info("Starting epoch time of the current financial year: " + epochTime);
        
        return epochTime;
    }
    
    public static Long getStartingDateOfCurrentMonth() {
        // Get the current date
        LocalDate currentDate = LocalDate.now();

        // Get the starting date of the current month
        LocalDate currentMonthStartDate = YearMonth.from(currentDate).atDay(1);

        // Convert the LocalDate to epoch time in milliseconds with GMT timezone
        long epochTime = currentMonthStartDate.atStartOfDay(ZoneId.of("GMT")).toInstant().toEpochMilli();

        log.info("Starting epoch time of the current month: " + epochTime);
        
        return epochTime;
    }
}
