package org.egov.bpa.web.model.edcr;

import java.math.BigDecimal;
import java.util.List;

public class HeadRoom extends Measurement {

    private List<BigDecimal> headRoomDimensions;

    public List<BigDecimal> getHeadRoomDimensions() {
        return headRoomDimensions;
    }

    public void setHeadRoomDimensions(List<BigDecimal> headRoomDimensions) {
        this.headRoomDimensions = headRoomDimensions;
    }

}
