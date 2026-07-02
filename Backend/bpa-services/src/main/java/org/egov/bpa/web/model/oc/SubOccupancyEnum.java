package org.egov.bpa.web.model.oc;

public enum SubOccupancyEnum {

    A_AF("Apartment/Flat"),
    A_FH("Farm House"),
    A_HE("Hostel Educational"),
    A_PO("Professional Office"),
    A_R("Residential"),
    A_SA("Service Apartment"),
    A_SR("Special Residential"),
    A2("Old Age Home"),
    B_HEI("Higher Educational Institute"),
    B_PS("Primary school"),
    B2("Educational HighSchool"),
    C_DFPAB("Dispensary for pet animals and birds"),
    C_HOTHC("Hospital/Tertiary Health care Centre"),
    C_MA("Medical Admin"),
    C_MIP("Medical IP"),
    C_MOP("Medical OP"),
    C_NAPI("Nursing and Paramedic Institute"),
    C_OHF("Other Health Facilities"),
    C_VH("Veterinary Hospital for pet animals and birds"),
    D_A("Religious"),
    D_AW("Assembly Worship"),
    D_B("At sub city level in urban extension"),
    D_BT("Bus Terminal"),
    D_C("Anganwari"),
    E_CLG("College"),
    E_EARC("Academic, including administration"),
    E_NS("Nursery Schools"),
    E_OB("Office/Business"),
    E_PS("Primary School"),
    E_SACA("Sports and Cultural Activities"),
    E_SFDAP("School for differently abled persons"),
    E_SFMC("School for Mentally Challenged"),
    F_CB("Commercial Building"),
    F_H("Hotels"),
    F_IT("IT/ITES Building"),
    F_K("Kiosk"),
    F_LD("Lodges"),
    F_PA("Parking Appurtenant"),
    F_PP("Parking Plaza"),
    F_RT("Restaurants"),
    G_LI("Large Industrial"),
    G_NPHI("Non-polluting and household industries"),
    G_PHI("Polluting and hazardous industries"),
    G_SI("Small Industrial"),
    H_PP("Petrol Pump"),
    H_S("Storage"),
    I_1("Hazardous (I1)"),
    I_2("Hazardous (I2)"),
    S_BH("Banquet hall"),
    S_CA("Cultural activities"),
    S_CRC("Community Recreational Club"),
    S_ECFG("Exhibition cum Fair Ground"),
    S_ICC("International Convention Centre"),
    S_MCH("Multipurpose Community Hall"),
    S_SAS("Sports and amenity structures"),
    S_SC("Science centre");

	private final String value;

	SubOccupancyEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	public static SubOccupancyEnum fromString(String value) {
		for (SubOccupancyEnum subOccupancyEnum : SubOccupancyEnum.values()) {
			if (subOccupancyEnum.value.equals(value)) {
				return subOccupancyEnum;
			}
		}
		throw new IllegalArgumentException("No enum constant for value: " + value);
	}

}
