package org.egov.edcr.constants;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DxfFileConstants {

	// occupancies code
	public static final String A = "A"; // Residential
	public static final String B = "B"; // Commercial
	public static final String C = "C"; // Public-Semi Public/Institutional
	public static final String D = "D"; // Public Utility
	public static final String E = "E"; // Industrial Zone
	public static final String F = "F"; // Education
	public static final String G = "G"; // Transportation
	public static final String H = "H"; // Agriculture
	public static final String M = "M"; // MIXED USE
	public static final String AF="AF"; //Additional Feature
	
	public static final String M_M = "M-M";
	public static final String MIXED_USE = "M-M"; 
	public static final int MIXED_USE_COLOR_CODE = -1;
	
	//version 2
	public static final String OC_RESIDENTIAL = "A"; // Residential
	public static final String OC_COMMERCIAL= "B"; // Commercial
	public static final String OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL= "C"; // Public-Semi Public/Institutional
	public static final String OC_PUBLIC_UTILITY = "D"; // Public Utility
	public static final String OC_INDUSTRIAL_ZONE = "E"; // Industrial Zone
	public static final String OC_EDUCATION= "F"; // Education
	public static final String OC_TRANSPORTATION= "G"; // Transportation
	public static final String OC_AGRICULTURE = "H"; // Agriculture
	public static final String OC_MIXED_USE = "M"; // MIXED USE
	public static final String OC_ADDITIONAL_FEATURE="AF";//ADDITIONAL FEATURE

	// sub occupancies code
	public static final String A_P = "A-P";// Plotted Detached/Individual Residential building
	public static final String A_S = "A-S";// Semi-detached
	public static final String A_R = "A-R";// Row housing
	public static final String A_AB = "A-AB";// Apartment Building
	public static final String A_HP = "A-HP";// Housing Project
	public static final String A_SA = "A-SA";// Studio Apartments
	public static final String A_DH = "A-DH";// Dharmasala
	public static final String A_D = "A-D";// Dormitory
	public static final String A_E = "A-E";// EWS
	public static final String A_LIH = "A-LIH";// Low Income Housing
	public static final String A_MIH = "A-MIH";// Medium Income Housing
	public static final String A_H = "A-H";// Hostel
	public static final String A_SH = "A-SH";// Shelter House
	public static final String A_SQ = "A-SQ";// Staff Qaurter
	
	public static final String PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING = "A-P";// Plotted Detached/Individual Residential building
	public static final String SEMI_DETACHED = "A-S";// Semi-detached
	public static final String ROW_HOUSING = "A-R";// Row housing
	public static final String APARTMENT_BUILDING = "A-AB";// Apartment Building
	public static final String HOUSING_PROJECT = "A-HP";// Housing Project
	public static final String STUDIO_APARTMENTS = "A-SA";// Studio Apartments
	public static final String DHARMASALA = "A-DH";// Dharmasala
	public static final String DORMITORY = "A-D";// Dormitory
	public static final String EWS = "A-E";// EWS
	public static final String LOW_INCOME_HOUSING = "A-LIH";// Low Income Housing
	public static final String MEDIUM_INCOME_HOUSING = "A-MIH";// Medium Income Housing
	public static final String HOSTEL = "A-H";// Hostel
	public static final String SHELTER_HOUSE = "A-SH";// Shelter House
	public static final String STAFF_QAURTER = "A-SQ";// Staff Qaurter

	public static final String B_H = "B-H";// Hotel
	public static final String B_5S = "B-5S";// 5 Star Hotel
	public static final String B_M = "B-M";// Motels
	public static final String B_SFH = "B-SHF";// Services for households
	public static final String B_B = "B-B";// Bank
	public static final String B_R = "B-R";// Resorts
	public static final String B_IIR = "B-IIR";// Lagoons and Lagoon Resort
	public static final String B_AB = "B-AB";// Amusement Building/Park and water sports
	public static final String B_F = "B-F";// Financial services and Stock exchanges
	public static final String B_C = "B-C";// Cold Storage and Ice Factory
	public static final String B_CBO = "B-CBO";// Commercial and Business Offices/Complex
	public static final String B_CNS = "B-CNS";// Convenience and Neighborhood Shopping
	public static final String B_P = "B-P";// Professional offices
	public static final String B_D = "B-D";// Departmental store
	public static final String B_GG = "B-GG";// Gas Godown
	public static final String B_G = "B-G";// Godowns
	public static final String B_GS = "B-GS";// Good Storage
	public static final String B_GH = "B-GH";// Guest Houses
	public static final String B_HR = "B-HR";// Holiday Resort
	public static final String B_BLH = "B-BLH";// Boarding and lodging houses
	public static final String B_P1 = "B-P1";// Petrol Pump (Only Filling Station)
	public static final String B_P2 = "B-P2";// Petrol Pump (Filling Station and Service station)
	public static final String B_CMS = "B-CMS";// CNG Mother Station
	public static final String B_RES = "B-RES";// Restaurant
	public static final String B_LS = "B-LS";// local(retail) shopping
	public static final String B_SC = "B-SC";// Shopping Center
	public static final String B_SM = "B-SM";// Shopping Mall 
	public static final String B_S = "B-S";// Showroom
	public static final String B_WST1 = "B-WST1";// Wholesale Storage (Perishable)
	public static final String B_WST2 = "B-WST2";// Wholesale Storage (Non Perishable)
	public static final String B_ST = "B-ST";// Storage/ Hangers/ Terminal Depot
	public static final String B_SUP = "B-SUP";// Supermarkets
	public static final String B_WH = "B-WH";// Ware House
	public static final String B_WM = "B-WM";// Wholesale Market
	public static final String B_MC = "B-MC";// media centres
	public static final String B_FC = "B-FC";// food courts
	public static final String B_WB = "B-WB";// Weigh bridges
	public static final String B_ME = "B-ME";// Mercentile
	
	public static final String HOTEL = "B-H";// Hotel
	public static final String FIVE_STAR_HOTEL = "B-5S";// 5 Star Hotel
	public static final String MOTELS = "B-M";// Motels
	public static final String SERVICES_FOR_HOUSEHOLDS = "B-SHF";// Services for households
	public static final String BANK = "B-B";// Bank
	public static final String RESORTS = "B-R";// Resorts
	public static final String LAGOONS_AND_LAGOON_RESORT = "B-IIR";// Lagoons and Lagoon Resort
	public static final String AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS = "B-AB";// Amusement Building/Park and water sports
	public static final String FINANCIAL_SERVICES_AND_STOCK_EXCHANGES = "B-F";// Financial services and Stock exchanges
	public static final String COLD_STORAGE_AND_ICE_FACTORY = "B-C";// Cold Storage and Ice Factory
	public static final String COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX = "B-CBO";// Commercial and Business Offices/Complex
	public static final String CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING = "B-CNS";// Convenience and Neighborhood Shopping
	public static final String PROFESSIONAL_OFFICES = "B-P";// Professional offices
	public static final String DEPARTMENTAL_STORE = "B-D";// Departmental store
	public static final String GAS_GODOWN = "B-GG";// Gas Godown
	public static final String GODOWNS = "B-G";// Godowns
	public static final String GOOD_STORAGE = "B-GS";// Good Storage
	public static final String GUEST_HOUSES = "B-GH";// Guest Houses
	public static final String HOLIDAY_RESORT = "B-HR";// Holiday Resort
	public static final String BOARDING_AND_LODGING_HOUSES = "B-BLH";// Boarding and lodging houses
	public static final String PETROL_PUMP_ONLY_FILLING_STATION = "B-P1";// Petrol Pump (Only Filling Station)
	public static final String PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION = "B-P2";// Petrol Pump (Filling Station and Service station)
	public static final String CNG_MOTHER_STATION = "B-CMS";// CNG Mother Station
	public static final String RESTAURANT = "B-RES";// Restaurant
	public static final String LOCAL_RETAIL_SHOPPING = "B-LS";// local(retail) shopping
	public static final String SHOPPING_CENTER = "B-SC";// Shopping Center
	public static final String SHOPPING_MALL = "B-SM";// Shopping Mall 
	public static final String SHOWROOM = "B-S";// Showroom
	public static final String WHOLESALE_STORAGE_PERISHABLE = "B-WST1";// Wholesale Storage (Perishable)
	public static final String WHOLESALE_STORAGE_NON_PERISHABLE = "B-WST2";// Wholesale Storage (Non Perishable)
	public static final String STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT = "B-ST";// Storage/ Hangers/ Terminal Depot
	public static final String SUPERMARKETS = "B-SUP";// Supermarkets
	public static final String WARE_HOUSE = "B-WH";// Ware House
	public static final String WHOLESALE_MARKET = "B-WM";// Wholesale Market
	public static final String MEDIA_CENTRES = "B-MC";// media centres
	public static final String FOOD_COURTS = "B-FC";// food courts
	public static final String WEIGH_BRIDGES = "B-WB";// Weigh bridges
	public static final String MERCENTILE = "B-ME";// Mercentile

	public static final String C_A = "C-A";// Auditorium
	public static final String C_B = "C-B";// Banquet Hall
	public static final String C_C = "C-C";// Cinema
	public static final String C_CL = "C-CL";// Club
	public static final String C_MP = "C-MP";// music pavilions
	public static final String C_CH = "C-CH";// Community Hall
	public static final String C_O = "C-O";// Orphanage
	public static final String C_OAH = "C-OAH";// Old Age Home
	public static final String C_SC = "C-SC";// Science Centre/Museum
	public static final String C_C1H = "C-C1H";// Confernce Hall
	public static final String C_C2H = "C-C2H";// Convention Hall
	public static final String C_SCC = "C-SCC";// sculpture complex
	public static final String C_CC = "C-CC";// Cultural Complex
	public static final String C_EC = "C-EC";// Exhibition Center
	public static final String C_G = "C-G";// Gymnasia
	public static final String C_MH = "C-MH";// Marriage Hall/Kalyan Mandap
	public static final String C_ML = "C-ML";// Multiplex
	public static final String C_M = "C-M";// Musuem
	public static final String C_PW = "C-PW";// Place of workship
	public static final String C_PL = "C-PL";// Public Libraries
	public static final String C_REB = "C-REB";// Recreation Bldg
	public static final String C_SPC = "C-SPC";// Sports Complex
	public static final String C_S = "C-S";// Stadium
	public static final String C_T = "C-T";// Theatre
	public static final String C_AB = "C-AB";// Administrative Buildings
	public static final String C_GO = "C-GO";// Government offices
	public static final String C_LSGO = "C-LSGO";// Local and semi Government offices
	public static final String C_P = "C-P";// Police/Army/Barrack
	public static final String C_RB = "C-RB";// Religious Building
	public static final String C_SWC = "C-SWC";// Social and welfare centres
	public static final String C_CI = "C-CI";// Clinic
	public static final String C_D = "C-D";// Dispensary
	public static final String C_YC = "C-YC";// Yoga Center
	public static final String C_DC = "C-DC";// Diagnostic Centre
	public static final String C_GSGH = "C-GSGH";// Govt-Semi Govt. Hospital
	public static final String C_RT = "C-RT";// Registered Trust
	public static final String C_HC = "C-HC";// Health centre
	public static final String C_H = "C-H";// Hospital
	public static final String C_L = "C-L";// Lab
	public static final String C_MTH = "C-MTH";// Maternity Home
	public static final String C_MB = "C-MB";// Medical Building
	public static final String C_NH = "C-NH";// Nursing Home
	public static final String C_PLY = "C-PLY";// Polyclinic
	public static final String C_RC = "C-RC";// Rehabilitaion Center
	public static final String C_VHAB = "C-VHAB";// Veterinary Hospital for pet animals and birds
	public static final String C_RTI = "C-RTI";// Research and Training Institute
	public static final String C_PS = "C-PS";// Police Station
	public static final String C_FS = "C-FS";// Fire Station
	public static final String C_J = "C-J";// Jail/Prison
	public static final String C_PO = "C-PO";// Post Office

	public static final String AUDITORIUM = "C-A";// Auditorium
	public static final String BANQUET_HALL = "C-B";// Banquet Hall
	public static final String CINEMA = "C-C";// Cinema
	public static final String CLUB = "C-CL";// Club
	public static final String MUSIC_PAVILIONS = "C-MP";// music pavilions
	public static final String COMMUNITY_HALL = "C-CH";// Community Hall
	public static final String ORPHANAGE = "C-O";// Orphanage
	public static final String OLD_AGE_HOME = "C-OAH";// Old Age Home
	public static final String SCIENCE_CENTRE_OR_MUSEUM = "C-SC";// Science Centre/Museum
	public static final String CONFERNCE_HALL = "C-C1H";// Confernce Hall
	public static final String CONVENTION_HALL = "C-C2H";// Convention Hall
	public static final String SCULPTURE_COMPLEX = "C-SCC";// sculpture complex
	public static final String CULTURAL_COMPLEX = "C-CC";// Cultural Complex
	public static final String EXHIBITION_CENTER = "C-EC";// Exhibition Center
	public static final String GYMNASIA = "C-G";// Gymnasia
	public static final String MARRIAGE_HALL_OR_KALYAN_MANDAP = "C-MH";// Marriage Hall/Kalyan Mandap
	public static final String MULTIPLEX = "C-ML";// Multiplex
	public static final String MUSUEM = "C-M";// Musuem
	public static final String PLACE_OF_WORKSHIP = "C-PW";// Place of workship
	public static final String PUBLIC_LIBRARIES = "C-PL";// Public Libraries
	public static final String RECREATION_BLDG = "C-REB";// Recreation Bldg
	public static final String SPORTS_COMPLEX = "C-SPC";// Sports Complex
	public static final String STADIUM = "C-S";// Stadium
	public static final String THEATRE = "C-T";// Theatre
	public static final String ADMINISTRATIVE_BUILDINGS = "C-AB";// Administrative Buildings
	public static final String GOVERNMENT_OFFICES = "C-GO";// Government offices
	public static final String LOCAL_AND_SEMI_GOVERNMENT_OFFICES = "C-LSGO";// Local and semi Government offices
	public static final String POLICE_OR_ARMY_OR_BARRACK = "C-P";// Police/Army/Barrack
	public static final String RELIGIOUS_BUILDING = "C-RB";// Religious Building
	public static final String SOCIAL_AND_WELFARE_CENTRES = "C-SWC";// Social and welfare centres
	public static final String CLINIC = "C-CI";// Clinic
	public static final String DISPENSARY = "C-D";// Dispensary
	public static final String YOGA_CENTER = "C-YC";// Yoga Center
	public static final String DIAGNOSTIC_CENTRE = "C-DC";// Diagnostic Centre
	public static final String GOVT_SEMI_GOVT_HOSPITAL = "C-GSGH";// Govt-Semi Govt. Hospital
	public static final String REGISTERED_TRUST = "C-RT";// Registered Trust
	public static final String HEALTH_CENTRE = "C-HC";// Health centre
	public static final String HOSPITAL = "C-H";// Hospital
	public static final String LAB = "C-L";// Lab
	public static final String MATERNITY_HOME = "C-MTH";// Maternity Home
	public static final String MEDICAL_BUILDING = "C-MB";// Medical Building
	public static final String NURSING_HOME = "C-NH";// Nursing Home
	public static final String POLYCLINIC = "C-PLY";// Polyclinic
	public static final String REHABILITAION_CENTER = "C-RC";// Rehabilitaion Center
	public static final String VETERINARY_HOSPITAL_FOR_PET_ANIMALS_AND_BIRDS = "C-VHAB";// Veterinary Hospital for pet animals and birds
	public static final String RESEARCH_AND_TRAINING_INSTITUTE = "C-RTI";// Research and Training Institute
	public static final String POLICE_STATION = "C-PS";// Police Station
	public static final String FIRE_STATION = "C-FS";// Fire Station
	public static final String JAIL_OR_PRISON = "C-J";// Jail/Prison
	public static final String POST_OFFICE = "C-PO";// Post Office

	
	public static final String D_BCC = "D-BCC";// Bill Collection Center
	public static final String D_BTC = "D-BTC";// Broadcasting-Transmission Centre
	public static final String D_BCG = "D-BCG";// Burial and cremation grounds
	public static final String D_PDSS = "D-PDSS";// Public Distribution System Shop
	public static final String D_PTPA = "D-PTPA";// Public Toilets in Public Area
	public static final String D_PUB = "D-PUB";// Public Utility Bldg
	public static final String D_SS = "D-SS";// Sub-Station
	public static final String D_TEL = "D-TEL";// Telecommunication
	public static final String D_WPS = "D-WPS";// water pumping stations
	public static final String D_SSY = "D-SSY";// service and storage yards
	public static final String D_EDD = "D-EDD";// electrical distribution depots
	
	public static final String BILL_COLLECTION_CENTER = "D-BCC";// Bill Collection Center
	public static final String BROADCASTING_TRANSMISSION_CENTRE = "D-BTC";// Broadcasting-Transmission Centre
	public static final String BURIAL_AND_CREMATION_GROUNDS = "D-BCG";// Burial and cremation grounds
	public static final String PUBLIC_DISTRIBUTION_SYSTEM_SHOP = "D-PDSS";// Public Distribution System Shop
	public static final String PUBLIC_TOILETS_IN_PUBLIC_AREA = "D-PTPA";// Public Toilets in Public Area
	public static final String PUBLIC_UTILITY_BLDG = "D-PUB";// Public Utility Bldg
	public static final String SUB_STATION = "D-SS";// Sub-Station
	public static final String TELECOMMUNICATION = "D-TEL";// Telecommunication
	public static final String WATER_PUMPING_STATIONS = "D-WPS";// water pumping stations
	public static final String SERVICE_AND_STORAGE_YARDS = "D-SSY";// service and storage yards
	public static final String ELECTRICAL_DISTRIBUTION_DEPOTS = "D-EDD";// electrical distribution depots

	public static final String E_IB = "E-IB";// Industrial Buildings (Factories, Workshops, etc.)
	public static final String E_NPI = "E-NPI";// Non Polluting Industrial
	public static final String E_ITB = "E-ITB";// IT, ITES Buildings
	public static final String E_SI = "E-SI";// SEZ Industrial
	public static final String E_L = "E-L";// Loading/Unloading Spaces
	public static final String E_FF = "E-FF";// Flatted Factory
	public static final String E_SF = "E-SF";// small factories and etc falls in industrial

	public static final String INDUSTRIAL_BUILDINGS_FACTORIES_WORKSHOPS_ETC = "E-IB";// Industrial Buildings (Factories, Workshops, etc.)
	public static final String NON_POLLUTING_INDUSTRIAL = "E-NPI";// Non Polluting Industrial
	public static final String IT_ITES_BUILDINGS = "E-ITB";// IT, ITES Buildings
	public static final String SEZ_INDUSTRIAL = "E-SI";// SEZ Industrial
	public static final String LOADING_OR_UNLOADING_SPACES = "E-L";// Loading/Unloading Spaces
	public static final String FLATTED_FACTORY = "E-FF";// Flatted Factory
	public static final String SMALL_FACTORIES_AND_ETC_FALLS_IN_INDUSTRIAL = "E-SF";// small factories and etc falls in industrial

	
	public static final String F_CC = "F-CC";// Coaching Centre
	public static final String F_CI = "F-CI";// Commercial Institute
	public static final String F_C = "F-C";// College
	public static final String F_CTI = "F-CTI";// Computer Training Institute
	public static final String F_NS = "F-NS";// Nursery School
	public static final String F_PS = "F-PS";// Primary School
	public static final String F_H = "F-H";// Hostel (Captive)
	public static final String F_HS = "F-HS";// High School
	public static final String F_PLS = "F-PLS";// Play School
	public static final String F_CR = "F-CR";// crÃ¨che
	public static final String F_SMC = "F-SMC";// School for Mentally Challenged.
	public static final String F_AA = "F-AA";// Art academy
	public static final String F_TC = "F-TC";// Technical College
	public static final String F_STC = "F-STC";// Sports training centers
	public static final String F_TI = "F-TI";// Training Institute
	public static final String F_VI = "F-VI";// Veterinary Institute
	public static final String F_MC = "F-MC";// Medical College
	public static final String F_RTC = "F-RTC";// Research and Training Center

	public static final String COACHING_CENTRE = "F-CC";// Coaching Centre
	public static final String COMMERCIAL_INSTITUTE = "F-CI";// Commercial Institute
	public static final String COLLEGE = "F-C";// College
	public static final String COMPUTER_TRAINING_INSTITUTE = "F-CTI";// Computer Training Institute
	public static final String NURSERY_SCHOOL = "F-NS";// Nursery School
	public static final String PRIMARY_SCHOOL = "F-PS";// Primary School
	public static final String HOSTEL_CAPTIVE = "F-H";// Hostel (Captive)
	public static final String HIGH_SCHOOL = "F-HS";// High School
	public static final String PLAY_SCHOOL = "F-PLS";// Play School
	public static final String CRECHE = "F-CR";// creche
	public static final String SCHOOL_FOR_MENTALLY_CHALLENGED = "F-SMC";// School for Mentally Challenged.
	public static final String ART_ACADEMY = "F-AA";// Art academy
	public static final String TECHNICAL_COLLEGE = "F-TC";// Technical College
	public static final String SPORTS_TRAINING_CENTERS = "F-STC";// Sports training centers
	public static final String TRAINING_INSTITUTE = "F-TI";// Training Institute
	public static final String VETERINARY_INSTITUTE = "F-VI";// Veterinary Institute
	public static final String MEDICAL_COLLEGE = "F-MC";// Medical College
	public static final String RESEARCH_AND_TRAINING_CENTER = "F-RTC";// Research and Training Center

	
	public static final String G_A = "G-A";// Airport
	public static final String G_AS = "G-AS";// Auto Stand
	public static final String G_MS = "G-MS";// Metro Station
	public static final String G_BS = "G-BS";// Bus Stand
	public static final String G_BT = "G-BT";// Bus Terminal
	public static final String G_I = "G-I";// ISBT
	public static final String G_RS = "G-RS";// Railway station
	public static final String G_TS = "G-TS";// Taxi Stand
	public static final String G_MLCP = "G-MLCP";// Multi Level Car Parking
	public static final String G_PP = "G-PP";// Public Parking
	public static final String G_TP = "G-TP";// Toll Plaza
	public static final String G_TT = "G-TT";// Truck Terminal

	public static final String AIRPORT = "G-A";// Airport
	public static final String AUTO_STAND = "G-AS";// Auto Stand
	public static final String METRO_STATION = "G-MS";// Metro Station
	public static final String BUS_STAND = "G-BS";// Bus Stand
	public static final String BUS_TERMINAL = "G-BT";// Bus Terminal
	public static final String ISBT = "G-I";// ISBT
	public static final String RAILWAY_STATION = "G-RS";// Railway station
	public static final String TAXI_STAND = "G-TS";// Taxi Stand
	public static final String MULTI_LEVEL_CAR_PARKING = "G-MLCP";// Multi Level Car Parking
	public static final String PUBLIC_PARKING = "G-PP";// Public Parking
	public static final String TOLL_PLAZA = "G-TP";// Toll Plaza
	public static final String TRUCK_TERMINAL = "G-TT";// Truck Terminal

	
	public static final String H_AF = "H-AF";// Agriculture Farm
	public static final String H_AG = "H-AG";// Agro Godown
	public static final String H_ARF = "H-ARF";// Agro-Research Farm
	public static final String H_FH = "H-FH";// Farm House
	public static final String H_CH = "H-CH";// Country Homes
	public static final String H_NGH = "H-NGH";// Nursery and green houses
	public static final String H_PDS = "H-PDS";// Polutry, Diary and Swine/Goat/Horse
	public static final String H_H = "H-H";// Horticulture
	public static final String H_SC = "H-SC";// Seri culture
	
	public static final String AGRICULTURE_FARM = "H-AF";// Agriculture Farm
	public static final String AGRO_GODOWN = "H-AG";// Agro Godown
	public static final String AGRO_RESEARCH_FARM = "H-ARF";// Agro-Research Farm
	public static final String FARM_HOUSE = "H-FH";// Farm House
	public static final String COUNTRY_HOMES = "H-CH";// Country Homes
	public static final String NURSERY_AND_GREEN_HOUSES = "H-NGH";// Nursery and green houses
	public static final String POLUTRY_DIARY_AND_SWINE_OR_GOAT_OR_HORSE = "H-PDS";// Polutry, Diary and Swine/Goat/Horse
	public static final String HORTICULTURE = "H-H";// Horticulture
	public static final String SERI_CULTURE = "H-SC";// Seri culture
	
//	public static final String AF_OH="AF-OH";	//Outhouse
//	public static final String AF_PW="AF-PW";	//Outhouse
	
	public static final String OUTHOUSE="AF-OH";	//Outhouse
	public static final String PUBLIC_WASHROOMS="AF-PW";	//Public Washrooms
	public static final String ACCOMODATION_OF_WATCH_AND_WARD_MAINTENANCE_STAFF="AF-AWWS";	//Accommodation of watch and ward/maintenance staff
	public static final String INFO_COMMS_SYSTEM = "AF-ICT";

	public static final String VERT_CLEAR_OHE = "VERT_CLEAR_OHEL";
	public static final String REAR_YARD = "REAR_YARD";
	public static final String BUILDING_FOOT_PRINT = "BLDG_FOOT_PRINT";
	public static final String SIDE_YARD_2 = "SIDE_YARD2";
	public static final String SIDE_YARD_1 = "SIDE_YARD1";
	public static final String FRONT_YARD = "FRONT_YARD";
	public static final String NOTIFIED_ROADS = "NOTIFIED_ROAD";
	public static final String NON_NOTIFIED_ROAD = "NON_NOTIFIED_ROAD";
	public static final String CULD_1 = "CULD_1";
	public static final String LANE_1 = "LANE_1";
	public static final String HORIZ_CLEAR_OHE2 = "HORIZ_CLEAR_OHEL";
	public static final String PLOT_BOUNDARY = "PLOT_BOUNDARY";
	public static final String LAYER_EXIT_WIDTH_DOOR = "BLK_%s_FLR_%s_EXIT_WIDTH_DOOR";
	public static final String LAYER_EXIT_WIDTH_STAIR = "BLK_%s_FLR_%s_EXIT_WIDTH_STAIR";
	public static final String LAYER_OHEL = "OHEL";
	public static final String LAYER_BIOMETRIC_WASTE_TREATMENT = "BIOMETRIC_WASTE_MNGMNT";
	public static final String LAYER_MEZZANINE_FLOOR_BLT_UP_AREA = "BLK_%s_FLR_%s_M_%s_BLT_UP_AREA";
	public static final String LAYER_MEZZANINE_FLOOR_DEDUCTION = "BLK_%s_FLR_%s_M_%s_BLT_UP_AREA_DEDUCT";
	public static final String LAYER_MEZZANINE_HALL_BLT_UP_AREA = "BLK_%s_FLR_%s_HALL_%s_BLT_UP_AREA";
	public static final String LAYER_MEZZANINE_HALL_DEDUCTION = "BLK_%s_FLR_%s_HALL_%s_BLT_UP_AREA_DEDUCT";
	public static final String LAYER_HGHT_ROOM = "BLK_%s_FLR_%s_HT_ROOM";
	public static final String LAYER_TRAVEL_DIST_TO_EXIT = "DIST_EXIT";
	public static final String LAYER_DA_RAMP = "BLK_%s_DA_RAMP";
	public static final String LAYER_DA_ROOM = "BLK_%s_FLR_%s_DA_ROOM";
	public static final String LAYER_LIFT = "BLK_%s_FLR_%s_LIFT";
	public static final String LAYER_DEPTH_CUTTING = "DEPTH_CUTTING";
	public static final String LAYER_ACCESSORY_BUILDING = "ACCBLK";
	public static final String DEPTH_CUTTING = "DEPTH_CUTTING_MORE_THAN_1.5_M";
	public static final String LAYER_RAMP = "BLK_%s_FLR_%s_RAMP";
	public static final String LAYER_ACCESSORY_SHORTEST_DISTANCE = "ACC_SHORTEST_DIST_TO_ROAD";
	public static final String LAYER_ACCESSORY_DIST_TO_PLOT_BNDRY = "ACCBLK_%s_DIST_BOUNDARY";
	public static final String LAYER_EXISTING_BLT_UP_AREA_DEDUCT = "BLK_%s_FLR_%s_BLT_UP_AREA_DEDUCT_EXISTING";
	public static final String LAYER_CANOPY = "DIST_CANOPY";
	public static final String FLOOR_HEIGHT_PREFIX = "FLOOR_HEIGHT";
	public static final String GOVERNMENT_AIDED = "WHETHER_GOVT_OR_AIDED_SCHOOL";
	public static final String LAYER_RAMP_WITH_NO = "BLK_%s_FLR_%s_RAMP_%s";
	public static final String LAYER_VEHICLE_RAMP_WITH_NO = "BLK_%s_FLR_%s_VEHICLE_RAMP_%s";
	public static final String LAYER_LIFT_WITH_NO = "BLK_%s_FLR_%s_LIFT_%s";
	public static final String LAYER_PLINTH_HEIGHT = "BLK_%s_PLINTH_HEIGHT";

	public static final String CRZ_ZONE = "CRZ";
	public static final String PLOT_AREA = "PLOT_AREA_M2";
	public static final Object ARCHITECT_NAME = "ARCHITECT_NAME";
	public static final String SHORTEST_DISTANCE_TO_ROAD = "SHORTEST_DIST_TO_ROAD";
	public static final String PLAN_INFO = "PLAN_INFO";
	public static final String LAYER_NAME_WASTE_DISPOSAL = "WASTE_DISPOSAL";
	public static final String LAYER_NAME_WATER_CLOSET = "WATER_CLOSET";
	public static final String LAYER_NAME_URINAL = "URINAL";
	public static final String LAYER_NAME_WASH = "WASH";
	public static final String LAYER_NAME_BATH = "BATH";
	public static final String LAYER_NAME_WC_BATH = "WC_BATH";
	public static final String LAYER_NAME_DRINKING_WATER = "DRINKING_WATER";
	public static final String LAYER_NAME_SPECIAL_WATER_CLOSET = "SP_WC";
	public static final String LAYER_FIRESTAIR_FLOOR = "BLK_%s_FLR_%s_FIRESTAIR_%s";
	public static final String LAYER_FLOOR_BLT_UP = "BLK_%s_FLR_%s_BLT_UP_AREA";
	public static final String LAYER_FLOOR_SPIRAL_STAIR = "BLK_%s_FLR_%s_SPIRAL_FIRE_STAIR_%s";
	public static final String LAYER_FIRESTAIR_FLIGHT = "BLK_%s_FLR_%s_FIRESTAIR_%s_FLIGHT_%s";
	public static final String FLOOR_HEIGHT = "FLR_HT_M";
	public static final String LAYER_COVERAGE = "BLK_%s_COVERED_AREA";
	public static final String LAYER_COVERAGE_DEDUCT = "BLK_%s_COVERED_AREA_DEDUCT";

	public static final String LAYER_STAIR_FLOOR = "BLK_%s_FLR_%s_STAIR_%s";
	public static final String LAYER_STAIR_FLIGHT = "BLK_%s_FLR_%s_STAIR_%s_FLIGHT_%s";

	public static final String ACCESS_WIDTH = "ACCESS_WIDTH_M";
	public static final String HEIGHT_OF_BUILDING = "HT_OF_BLDG";
	public static final String BUILT_UP_AREA = "BLT_UP_AREA";
	public static final int BLDG_EXTERIOR_WALL_COLOR = 2;
	public static final String FAR_DEDUCT = "FAR_DEDUCT";
	public static final int FAR_DEDUCT_COLOR = 2;
	public static final String COVERGAE_DEDUCT = "COVERAGE_DEDUCT"; // not used
																	// in PHASE2
	public static final String BUILT_UP_AREA_DEDUCT = "BLT_UP_AREA_DEDUCT";
	public static final String SECURITY_ZONE = "SECURITY_ZONE";
	public static final String FLOOR_AREA = "FLOOR_AREA";
	public static final String OCCUPANCY = "OCCUPANCY";
	public static final String FLOOR_NAME_PREFIX = "FLR_";
	public static final String BLOCK_NAME_PREFIX = "BLK_";
	public static final String EXISTING_PREFIX = "_EXISTING";
	public static final String LEVEL_NAME_PREFIX = "LVL_";
	public static final int HABITABLE_ROOM_COLOR = 4;
	public static final int FLOOR_EXTERIOR_WALL_COLOR = 5;
	public static final int FLOOR_OPENSPACE_COLOR = 6;
	public static final String SHADE_OVERHANG = "SHADE_OVERHANG";
	public static final String OPEN_STAIR = "OPEN_STAIR";
	public static final String DIST_CL_ROAD = "DIST_CL_ROAD";
	public static final String OPENING_BELOW_2_1_ON_SIDE_LESS_1M = "OPENING_BELOW_2.1_ON_SIDE_LESS_1M_OR_LESS_EQUALTO_0.6M";
	public static final String OPENING_BELOW_2_1_ON_REAR_LESS_1M = "OPENING_BELOW_2.1_ON_REAR_LESS_1M";
	public static final String OPENING_ABOVE_2_1_ON_SIDE_LESS_1M = "OPENING_ABOVE_2.1_ON_SIDE_LESS_1M_OR_LESS_EQUALTO_0.6M";
	public static final String OPENING_ABOVE_2_1_ON_REAR_LESS_1M = "OPENING_ABOVE_2.1_ON_REAR_LESS_1M";

	public static final String NOC_TO_ABUT_SIDE = "NOC_TO_ABUT_SIDE";
	public static final String NOC_TO_ABUT_REAR = "NOC_TO_REAR_SIDE";
	public static final String MAX_HEIGHT_CAL = "MAX_HEIGHT_CAL";
	public static final String BSMNT_REAR_YARD = "BSMNT_REAR_YARD";
	public static final String BSMNT_FOOT_PRINT = "BSMNT_FOOT_PRINT";
	public static final String BSMNT_SIDE_YARD_1 = "BSMNT_SIDE_YARD_1";
	public static final String BSMNT_SIDE_YARD_2 = "BSMNT_SIDE_YARD_2";
	public static final String BSMNT_FRONT_YARD = "BSMNT_FRONT_YARD";
	public static final String RESI_UNIT = "RESI_UNIT";
	public static final String RESI_UNIT_DEDUCT = "RESI_UNIT_DEDUCT";
	public static final String PARKING_SLOT = "PARKING_SLOT";
	public static final String UNITFA = "UNITFA";
	public static final String UNITFA_DEDUCT = "UNITFA_DEDUCT";
	public static final String UNITFA_HALL = "UNITFA_HALL";
	public static final String UNITFA_BALCONY = "UNITFA_BALCONY";
	public static final String UNITFA_DINING = "UNITFA_DINING";
	public static final String TWO_WHEELER_PARKING = "TWO_WHEELER_PARKING";
	public static final String LOADING_UNLOADING = "LOADING_UNLOADING";
	public static final String MECHANICAL_PARKING = "MECHANICAL_PARKING";
	public static final String MECH_PARKING = "MECH_PARKING";
	public static final String DA_PARKING = "DA_PARKING";
	public static final String SINGLE_FAMILY_BLDG = "SINGLE_FAMILY_BLDG";
	public static final String SEATS_SP_RESI = "SEATS_SP_RESI";
	public static final String LAYER_NAME_DIST_BETWEEN_BLOCKS = "DIST_BETWEEN_BLK_%s_BLK_%s";
	public static final int COLOUR_CODE_NOTIFIEDROAD = 1;
	public static final int COLOUR_CODE_NONNOTIFIEDROAD = 2;
	public static final int COLOUR_CODE_LANE = 5;
	public static final int COLOUR_CODE_CULDESAC = 6;
	public static final int COLOUR_CODE_WELLTOBOUNDARY = 7;
	public static final int COLOUR_CODE_WELLTOLEACHPIT = 8;
	public static final Integer COLOUR_CODE_LEACHPIT_TO_PLOT_BNDRY = 9;
	public static final int OCCUPANCY_A1_COLOR_CODE = 25;
	public static final int OCCUPANCY_A2_COLOR_CODE = 3;
	public static final int OCCUPANCY_B1_COLOR_CODE = 4;
	public static final int OCCUPANCY_B2_COLOR_CODE = 14;
	public static final int OCCUPANCY_B3_COLOR_CODE = 15;
	// public static final int OCCUPANCY_C_COLOR_CODE = 5;
	public static final int OCCUPANCY_D_COLOR_CODE = 6;
	public static final int OCCUPANCY_E_COLOR_CODE = 7;
	public static final int OCCUPANCY_F_COLOR_CODE = 8;
	public static final int OCCUPANCY_G1_COLOR_CODE = 9;
	public static final int OCCUPANCY_G2_COLOR_CODE = 10;
	public static final int OCCUPANCY_H_COLOR_CODE = 11;
	public static final int OCCUPANCY_I1_COLOR_CODE = 12;
	public static final int OCCUPANCY_I2_COLOR_CODE = 13;
	public static final int OCCUPANCY_D1_COLOR_CODE = 16;
	public static final int OCCUPANCY_A2_BOARDING_COLOR_CODE = 19;
	public static final int OCCUPANCY_C1_COLOR_CODE = 5;
	public static final int OCCUPANCY_C2_COLOR_CODE = 20;
	public static final int OCCUPANCY_C3_COLOR_CODE = 21;
	public static final int OCCUPANCY_D2_COLOR_CODE = 22;
	public static final int OCCUPANCY_F1_COLOR_CODE = 17;
	public static final int OCCUPANCY_F2_COLOR_CODE = 18;
	public static final int OCCUPANCY_F3_HOTEL_COLOR_CODE = 23;
	public static final int OCCUPANCY_A1_APARTMENT_COLOR_CODE = 2;
	public static final int OCCUPANCY_A1_PROFESSIONALOFFICE_COLOR_CODE = 24;
	public static final int OCCUPANCY_I2_KIOSK_COLOR_CODE = 26;

	public static final int MEZZANINE_HEAD_ROOM_COLOR_CODE = 1;
	public static final int NORMAL_ROOM_BCEFHI_OCCUPANCIES_COLOR_CODE = 2;
	public static final int AC_ROOM_BCEFHI_OCCUPANCIES_COLOR_CODE = 3;
	public static final int CAR_AND_TWO_WHEELER_PARKING_ROOM_COLOR_CODE = 4;
	public static final int ASSEMBLY_ROOM_COLOR_CODE = 5;
	public static final int ASSEMBLY_AC_HALL_COLOR_CODE = 6;
	public static final int HEAD_ROOM_BENEATH_OR_ABOVE_BALCONY_COLOR_CODE = 7;
	public static final int HEAD_ROOM_IN_GENERAL_AC_ROOM_IN_ASSEMBLY_OCCUPANCY_COLOR_CODE = 8;
	public static final int GENERALAC_STORE_TOILET_LAMBER_CELLAR_ROOM_COLOR_CODE = 9;
	public static final int WORK_ROOM_UNDER_OCCUPANCY_G_COLOR_CODE = 10;
	public static final int LAB_ENTRANCE_HALL_CANTEEN_CLOAK_ROOM_COLOR_CODE = 11;
	public static final int STORE_TOILET_ROOM_IN_INDUSTRIES_COLOR_CODE = 12;

	public static final int OCCUPANCY_A2_PARKING_WITHATTACHBATH_COLOR_CODE = 3;
	public static final int OCCUPANCY_A2_PARKING_WOATTACHBATH_COLOR_CODE = 23;
	public static final int OCCUPANCY_A2_PARKING_WITHDINE_COLOR_CODE = 24;

	// ***********START - Extra functionalities color code key names************
	// ******** Height of room related ************
	public static final String COLOR_KEY_MEZZANINE_HEAD_ROOM = "Mezzanine head room";
	public static final String COLOR_KEY_NORMAL_ROOM_BCEFHI_OCCUPANCIES = "Normal room for BCEFHI occupancies";
	public static final String COLOR_KEY_AC_ROOM_BCEFHI_OCCUPANCIES = "AC room for BCEFHI occupancies";
	public static final String COLOR_KEY_CAR_AND_TWO_WHEELER_PARKING_ROOM = "Car and two parking room";
	public static final String COLOR_KEY_ASSEMBLY_ROOM = "Assembly room";
	public static final String COLOR_KEY_ASSEMBLY_AC_HALL = "Assembly AC hall";
	public static final String COLOR_KEY_HEAD_ROOM_BENEATH_OR_ABOVE_BALCONY = "Head room beneath or above balcony";
	public static final String COLOR_KEY_HEAD_ROOM_IN_GENERAL_AC_ROOM_IN_ASSEMBLY_OCCUPANCY = "Head room in general AC room in assembly";
	public static final String COLOR_KEY_GENERALAC_STORE_TOILET_LAMBER_CELLAR_ROOM = "GeneralLac store toiler lambar cellar";
	public static final String COLOR_KEY_WORK_ROOM_UNDER_OCCUPANCY_G = "Work room under industrial";
	public static final String COLOR_KEY_LAB_ENTRANCE_HALL_CANTEEN_CLOAK_ROOM = "Lab entrance hall canteen cloak room";
	public static final String COLOR_KEY_STORE_TOILET_ROOM_IN_INDUSTRIES = "Store toilet room in industrial";
	public static final String COLOR_RESIDENTIAL_ROOM = "Residential room";
	public static final String COLOR_COMMERCIAL_ROOM = "Commercial room";
	public static final String COLOR_EDUCATIONAL_ROOM = "Educational room";
	public static final String COLOR_INDUSTRIAL_ROOM = "Industrial room";
	public static final String RESIDENTIAL_KITCHEN_ROOM_COLOR = "Residential kitchen room";
	public static final String RESIDENTIAL_KITCHEN_STORE_ROOM_COLOR = "Residential kitchen store room";
	public static final String RESIDENTIAL_KITCHEN_DINING_ROOM_COLOR = "Residential kitchen dining room";
	public static final String COMMERCIAL_KITCHEN_ROOM_COLOR = "Commercial kitchen room";
	public static final String COMMERCIAL_KITCHEN_STORE_ROOM_COLOR = "Commercial kitchen store room";
	public static final String COMMERCIAL_KITCHEN_DINING_ROOM_COLOR = "Commercial kitchen dining room";
	// ******** Sanitation related ************
	public static final String COLOR_KEY_MALE_WATER_CLOSET = "Male water closet";
	public static final String COLOR_KEY_FEMALE_WATER_CLOSET = "Female water closet";
	public static final String COLOR_KEY_COMMON_WATER_CLOSET = "Common water closet";

	// Yard related
	public static final String COLOR_KEY_YARD_DIMENSION = "Yard dimension";

	// ********Parking related************
	public static final String COLOR_KEY_A_SR_PARKING_WITHATTACHBATH = "Special residetial with attach bath";
	public static final String COLOR_KEY_A_SR_PARKING_WOATTACHBATH = "Special residetial without attach bath";
	public static final String COLOR_KEY_A_SR_PARKING_WITHDINE = "Special residetial with dine";

	// *******Stair related**************
	public static final String COLOR_KEY_FLIGHT_LENGTH = "Flight length";
	public static final String COLOR_KEY_FLIGHT_WIDTH = "Flight width";

	// ********Interior open space related*****
	public static final String COLOR_KEY_HABITABLE_ROOM = "Habitable room";
	public static final String COLOR_KEY_FLR_EXTERIOR_WALL = "Floor exterior wall";
	public static final String COLOR_KEY_FLR_OPEN_SPACE = "Floor open space";

	// ********Distances related*****
	public static final String COLOR_KEY_NOTIFIED_ROAD = "Notified road";
	public static final String COLOR_KEY_NON_NOTIFIED_ROAD = "Non notified road";
	public static final String COLOR_KEY_LANE = "lane";
	public static final String COLOR_KEY_CULDE_SAC_ROAD = "Culdesac road";
	public static final String COLOR_KEY_WELL_TO_BNDRY = "Well to boundary";
	public static final String COLOR_KEY_WELL_TO_LEACH_PIT = "Well to leach pit";
	public static final String COLOR_KEY_PIT_TO_PLOT_BNDRY = "Leach pit to plot boundary";
	// ***********END - Extra functionalities color code key names************

	public static final String MTEXT_NAME_HEIGHT_M = "HEIGHT_M";
	public static final int COLOR_CODE_MALE_WATER_CLOSET = 1;
	public static final int COLOR_CODE_FEMALE_WATER_CLOSET = 2;
	public static final int COLOR_CODE_COMMON_WATER_CLOSET = 3;
	public static final String LAYER_NAME_MAX_HEIGHT_CAL = "BLK_%s_MAX_HEIGHT_CAL";
	public static final String LAYER_NAME_MAX_HEIGHT_CAL_SET_BACK = "BLK_%s_MAX_HEIGHT_CAL_SET_BACK";
	public static final String INSITU_WASTE_TREATMENT_PLANT = "INSITU_WASTE_TREATMENT_PLANT";
	public static final String RECYCLING_WASTE_WATER = "RECYCLING_WASTE_WATER";
	public static final String LAYER_NAME_WELL = "WELL";
	public static final String DIST_WELL = "DIST_WELL";
	public static final String RAINWATER_HARWESTING = "RWH";
	public static final String SOLAR = "SOLAR";
	public static final String RWH_CAPACITY_L = "RWH_CAPACITY_L";
	public static final String SOLID_LIQUID_WASTE_TREATMENT = "SOLID_LIQUID_WASTE_TREATMENT";
	public static final String EXISTING_FLOOR_AREA_TO_BE_DEMOLISHED = "EXISTING_FLOOR_AREA_TO_BE_DEMOLISHED_M2";

	public static final String RESURVEY_NO = "RS_NO";
	public static final String REVENUE_WARD = "REVENUE_WARD";
	public static final String VILLAGE = "VILLAGE";
	public static final String DESAM = "DESAM";
	public static final Object NO_OF_BEDS = "NO_OF_BEDS";
	public static final int YARD_DIMENSION_COLOR = 2;

	public static final String AREA_TYPE = "Area Type";
	public static final String ROAD_WIDTH = "Road Width";
	public static final String COMMERCIAL = "COMMERCIAL";
	public static final String RULE_28 = "28";
	public static final String SETBACK = "SetBack";
	public static final String YARD_NAME = "Yard Name";

	public static final String OCCUPANCY_ALLOWED = "Only residential or commerical or industrial buildings will be scrutinized for now.";
	public static final String OCCUPANCY_ALLOWED_KEY = "occupancy_allowed";
	public static final String OCCUPANCY_PO_NOT_ALLOWED = "Plans with only professional office occupancy is not allowed";
	public static final String OCCUPANCY_PO_NOT_ALLOWED_KEY = "occupancy_po_not_allowed";
	public static final String RWH_DECLARED = "RWH_DECLARED";

	public static final String ANONYMOUS_APPLICANT = "ANONYMOUS";
	public static final String NEWCONSTRUCTION_SERVICE = "New Construction";
	public static final String MAINRIVER = "MainRiver";
	public static final String SUBRIVER = "SubRiver";

	private static final Map<String, String> SERVICE_TYPE = new ConcurrentHashMap<>();
	static {
		SERVICE_TYPE.put("NEW_CONSTRUCTION", "New Construction");
		SERVICE_TYPE.put("ADDITION_AND_ALTERATION", "Addition and Alteration");
	}
	
	//from api
	public static final String NEW_CONSTRUCTION="NEW_CONSTRUCTION";
	public static final String ADDITION_AND_ALTERATION="ALTERATION";
	public static final String ALTERATION_MSG1=" (According to ODA (P&BS) Rules 2020)";
	
	
	public static Map<String, String> getServiceTypeList() {
		return Collections.unmodifiableMap(SERVICE_TYPE);
	}
	
	public static final String NUMBER_OF_OCCUPANTS_OR_USERS_OR_BED_BLK="NUMBER_OF_OCCUPANTS_OR_USERS_OR_BED_BLK_%S";
	public static final String LAND_USE_ZONE="LAND_USE_ZONE";
	public static final String IS_BLOCK_S_HAVING_ENTIRE_FACADE_IN_GLASS="IS_BLOCK_%S_HAVING_ENTIRE_FACADE_IN_GLASS";
	
	//LAND_USE_ZONE
	public static final String RETAIL_COMMERCIAL_AND_BUSINESS_USE_ZONE="RETAIL COMMERCIAL & BUSINESS USE ZONE";
	public static final String WHOLESALE_COMMERCIAL_USE="WHOLESALE COMMERCIAL USE";
	public static final String INDUSTRIAL_USE_ZONE="INDUSTRIAL USE ZONE";
	public static final String PUBLIC_AND_SEMI_PUBLIC_USE_ZONES="PUBLIC & SEMI- PUBLIC USE ZONES";
	public static final String UTILITY_SERVICE_USE_ZONE="UTILITY & SERVICE USE ZONE";
	public static final String OPEN_SPACE_USE_ZONE="OPEN SPACE USE ZONE";
	public static final String TRANSPORTATION_USE="TRANSPORTATION USE";
	public static final String AGRICULTURE_AND_FOREST_USE_ZONE="AGRICULTURE & FOREST USE ZONE";
	public static final String WATER_BODIES_USE_ZONE="WATER BODIES USE ZONE";
	public static final String SPECIAL_HERITAGE_ZONE="SPECIAL HERITAGE ZONE";
	public static final String ENVIRONMENTALLY_SENSITIVE_ZONE="ENVIRONMENTALLY SENSITIVE ZONE";
	
	public static final String NA = "NA";
	public static final String YES="YES";
	public static final String NO="NO";
	
	public static final String IS_BUILDING_UNDER_HAZARDOUS_OCCUPANCY_CATEGORY="IS_BUILDING_UNDER_HAZARDOUS_OCCUPANCY_CATEGORY";
	public static final String PER_ACRE_BENCHMARK_VALUE_OF_LAND_NEEDED_IF_PROJECT_IS_HAVING_PURCHASABLE_FAR_COMPONENT="PER_ACRE_BENCHMARK_VALUE_OF_LAND_NEEDED_IF_PROJECT_IS_HAVING_PURCHASABLE_FAR_COMPONENT";
	public static final String TOTAL_NUMBER_OF_DWELLING_UNITS="TOTAL_NUMBER_OF_DWELLING_UNITS";
	
	public static final String METER="m";
	
	//Declaration in Scrutiny Report
	public static final String APPROVED_LAYOUT_DECLARATION="IS_THE_PLOT_PART_OF_THE_LAYOUT_APPROVED_BY_THE_AUTHORITY_OR_DEVELOPED_AND_ALLOTTED_BY_THE_GOVERNMENT_OR_STATUTORY_BODIES_OR_IS_A_FINAL_PLOT_IN_TOWN_PLANNING_SCHEMES_OR_DEVELOPMENT_SCHEMES";
	public static final String DOES_THE_PROJECT_REQUIRE_NOC_FROM_AAI_AS_PER_THE_COLOUR_CODED_ZONE_MAPS="DOES_THE_PROJECT_REQUIRE_NOC_FROM_AAI_AS_PER_THE_COLOUR_CODED_ZONE_MAPS";
	public static final String IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_CENTRALLY_PROTECTED_MONUMENT="IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_CENTRALLY_PROTECTED_MONUMENT";
	public static final String IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_STATE_PROTECTED_MONUMENT="IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_STATE_PROTECTED_MONUMENT";
	public static final String IS_THE_PROJECT_LOCATED_WITHIN_200_METERS_FROM_STRATEGIC_BUILDINGS="IS_THE_PROJECT_LOCATED_WITHIN_200_METERS_FROM_STRATEGIC_BUILDINGS";
	public static final String IS_PROPOSED_CONSTRUCTION_NEXT_TO_FLOOD_EMBANKMENT_AND_DOES_APPLICANT_WANT_TO_HAVE_DIRECT_ACCESS_FROM_THE_EMBANKMENT_ROAD="IS_PROPOSED_CONSTRUCTION_NEXT_TO_FLOOD_EMBANKMENT_AND_DOES_APPLICANT_WANT_TO_HAVE_DIRECT_ACCESS_FROM_THE_EMBANKMENT_ROAD";
	public static final String IS_KISAM_OF_LAND_RECORDED_AS_AGRICULTURE_IN_RECORD_OF_RIGHTS="IS_KISAM_OF_LAND_RECORDED_AS_AGRICULTURE_IN_RECORD_OF_RIGHTS";
	public static final String IS_THE_PROJECT_ADJACENT_TO_HIGHWAY_AND_HAVING_DIRECT_ACCESS_TO_IT="IS_THE_PROJECT_ADJACENT_TO_HIGHWAY_AND_HAVING_DIRECT_ACCESS_TO_IT";
	public static final String IS_THE_PROJECT_CLOSE_TO_THE_COASTAL_REGION="IS_THE_PROJECT_CLOSE_TO_THE_COASTAL_REGION";
	public static final String STAR_RATING_FOR_HOTEL_PROJECT="STAR_RATING_FOR_HOTEL_PROJECT";//NA 2,3
	public static final String DOES_HOSPITAL_HAVE_CRITICAL_CARE_UNIT="DOES_HOSPITAL_HAVE_CRITICAL_CARE_UNIT";
	public static final String IS_SECURITY_DEPOSIT_REQUIRED="IS_SECURITY_DEPOSIT_REQUIRED";
	public static final String DISTANCE_OF_DA_PARKING_SPACE_FROM_BUILDING_ENTRANCE="DISTANCE_OF_DA_PARKING_SPACE_FROM_BUILDING_ENTRANCE";
	public static final String TOTAL_PARKING_AREA_IF_PROJECT_HAS_OFF_SITE_PARKING_PROVISION_WITHIN_300_METERS_FROM_PROJECT_SITE="TOTAL_PARKING_AREA_IF_PROJECT_HAS_OFF_SITE_PARKING_PROVISION_WITHIN_300_METERS_FROM_PROJECT_SITE";
	public static final String HAS_PROJECT_PROVIDED_MIN_10_PER_BUA_FOR_EWS_WITHIN_5_KM_FROM_PROJECT_SITE="HAS_PROJECT_PROVIDED_MIN_10_PER_BUA_FOR_EWS_WITHIN_5_KM_FROM_PROJECT_SITE";
	public static final String ARCHITECT_OR_TECHNICAL_PERSON_NAME="ARCHITECT_OR_TECHNICAL_PERSON_NAME";
	public static final String IS_BUILDING_CENTRALLY_AIR_CONDITIONED="IS_BUILDING_CENTRALLY_AIR_CONDITIONED";
	public static final String DOES_PROJECT_HAVE_MORE_THAN_10000_LITRES_OF_WASTE_WATER_DISCHARGE_PER_DAY="DOES_PROJECT_HAVE_MORE_THAN_10000_LITRES_OF_WASTE_WATER_DISCHARGE_PER_DAY";
	public static final String IS_LAND_REGULARIZED="IS_LAND_REGULARIZED";
	public static final String IS_DRIVEWAY_PROVIDING_ACCESS_TO_REAR_SIDE_OR_ANY_OTHER_SIDE_OTHER_THAN_FRONT_OF_THE_BUILDING="IS_DRIVEWAY_PROVIDING_ACCESS_TO_REAR_SIDE_OR_ANY_OTHER_SIDE_OTHER_THAN_FRONT_OF_THE_BUILDING";
	public static final String PROVISION_FOR_HELIPAD_PRESENT="PROVISION_FOR_HELIPAD_PRESENT";
	public static final String IS_THE_PROJECT_LOCATED_IN_MIXED_USE_ZONE = "IS_THE_PROJECT_LOCATED_IN_MIXED_USE_ZONE";
	public static final String IS_THE_PROJECT_LOCATED_IN_TRANSIT_ORIENTED_ZONE = "IS_THE_PROJECT_LOCATED_IN_TRANSIT_ORIENTED_ZONE";
	public static final String PRINCIPAL_USE_IN_BUILDING_IF_PROJECT_IS_LOCATED_IN_MUZ_OR_TOZ = "PRINCIPAL_USE_IN_BUILDING_IF_PROJECT_IS_LOCATED_IN_MUZ_OR_TOZ";
	public static final String DO_YOU_WANT_TO_AVAIL_LIFT_COUNT_RELAXATION_AS_PER_SECTION_42_2_2_OF_ODA_RULES = "DO_YOU_WANT_TO_AVAIL_LIFT_COUNT_RELAXATION_AS_PER_SECTION_42_2_2_OF_ODA_RULES";
	public static final String IS_PROJECT_ONLY_LIG_OR_EWS = "IS_PROJECT_ONLY_LIG_OR_EWS";
	public static final String IS_THE_PROJECT_COMING_UNDER_AFFORDABLE_HOUSING_SCHEME_AS_PER_HOUSING_FOR_ALL_POLICY_2022 = "IS_THE_PROJECT_COMING_UNDER_AFFORDABLE_HOUSING_SCHEME_AS_PER_HOUSING_FOR_ALL_POLICY_2022";
	public static final String IS_ACCOMMODATION_OF_VEHICLE_DRIVERS_PROVIDED_WITHIN_SITE_OR_WITHIN_A_DISTANCE_OF_250_METERS_RADIUS_OF_THE_PREMISES = "IS_ACCOMMODATION_OF_VEHICLE_DRIVERS_PROVIDED_WITHIN_SITE_OR_WITHIN_A_DISTANCE_OF_250_METERS_RADIUS_OF_THE_PREMISES";
	public static final String REQUEST_FOR_DRAWING_REWORK_CATEGORY_EXEMPTION_DUE_TO_AIRPORT_PROPOSAL_IN_PURI = "REQUEST_FOR_DRAWING_REWORK_CATEGORY_EXEMPTION_DUE_TO_AIRPORT_PROPOSAL_IN_PURI";
	public static final String IS_PROJECT_COMING_UNDER_SHOP_CUM_RESIDENTIAL_OCCUPANCY = "IS_PROJECT_COMING_UNDER_SHOP_CUM_RESIDENTIAL_OCCUPANCY";
	public static final String IS_PROJECT_COMING_UNDER_WORK_CUM_RESIDENTIAL_OCCUPANCY = "IS_PROJECT_COMING_UNDER_WORK_CUM_RESIDENTIAL_OCCUPANCY";
	public static final String IS_EIDP_EXEMPTED_FOR_THE_PROJECT="IS_EIDP_EXEMPTED_FOR_THE_PROJECT";
	public static final String IS_THE_CWWC_FEE_ALREADY_PAID="IS_THE_CWWC_FEE_ALREADY_PAID";

	
	public static final String ARE_THE_PLOTS_ALLOTTED_IN_ROW = "ARE_THE_PLOTS_ALLOTTED_IN_ROW";
	
	
	public static final String LOW="LOW";
	public static final String HIGH="HIGH";
	public static final String OTHER_THAN_LOW="OTHER THAN LOW";
	
	
	public static final String SOLOR_WATER_HEATING_IN_LTR="SOLOR_WATER_HEATING_IN_LTR";
	public static final Object NUMBER_OF_STUDENTS = "NUMBER_OF_STUDENTS";
	public static final Object NUMBER_OF_BEDS = "NUMBER_OF_BEDS";
	public static final String WASTE_WATER_QUANTITY="WASTE_WATER_QUANTITY";

	public static final String OPTIONAL="OPTIONAL";
	public static final String MANDATORY="MANDATORY";
	public static final String PROVIDED="Provided";
	public static final String NOT_PROVIDED="Not Provided";
	
	public static final String COLOR_RESIDENTIAL_ROOM_NATURALLY_VENTILATED = "Habitable Room (Naturally Ventilated)";//1 - db
	public static final String COLOR_RESIDENTIAL_ROOM_MECHANICALLY_VENTILATED = "Habitable Room (Mechanically Ventilated)";//2 - db
	public static final String COLOR_PUBLIC_WASHROOM = "Public Washroom";//101 - db
	
	public static final String COLOR_STUDY_ROOM="Study Room";//3
	public static final String COLOR_LIBRARY_ROOM="Library Room";//4
	public static final String COLOR_GAME_ROOM="Game Room";//5
	public static final String COLOR_STORE_ROOM="Store Room";//6
	public static final String COLOR_GUARD_ROOM="Guard Room";//33
	public static final String COLOR_ELECTRIC_CABIN_ROOM="Electric Cabin";//34
	public static final String COLOR_SUB_STATION_ROOM="Sub-Station";//35
	public static final String COLOR_GYM_ROOM="Gym Room";//7

	public static final String COLOR_CCTV_ROOM="CCTV Room";//28 - db
	public static final String COLOR_SERVICE_ROOM="Service Room";//29 - db
	public static final String COLOR_MEP_ROOM="MEP Room";//30 - db
	public static final String COLOR_LIFT_LOBBY="Lift Lobby";//32 - db
	public static final String COLOR_STILT_FLOOR = "Stilt Floor";//38 - db
	public static final String COLOR_SERVICE_FLOOR = "Service Floor";//39 - db
	public static final String COLOR_LAUNDRY_ROOM="Laundry Room";//31 - db
	public static final String COLOR_GENERATOR_ROOM="Generator Room";//36 - db
	
	public static final String BPA_PA_MODULE_CODE = "BPA1";
	public static final String BPA_PO_MODULE_CODE = "BPA2";
	public static final String BPA_PM_MODULE_CODE = "BPA3";
	public static final String BPA_DP_BP_MODULE_CODE = "BPA4";
	public static final String BPA_APPROVAL_BY_AN_ACCREDITED_PERSON="BPA5";
	public static final String BPA_PRE_APPROVED_CODE = "BPA6";
	public static final String BPA_CATEGORY_A_MODULE_CODE = "BPA7";
	
	public static final String BPA_OC_PA_MODULE_CODE = "BPA_OC1";
	public static final String BPA_OC_PO_MODULE_CODE = "BPA_OC2";
	public static final String BPA_OC_PM_MODULE_CODE = "BPA_OC3";
	public static final String BPA_OC_DP_BP_MODULE_CODE = "BPA_OC4";
	
	public static final String MINIMUM_DISTANCE_FROM_THE_ROAD_INTERSECTIONS="MINIMUM_DISTANCE_FROM_THE_ROAD_INTERSECTIONS";
	public static final String MINIMUM_DISTANCE_OF_PROPERTY_LINE_FROM_THE_CENTRE_LINE_OF_THE_ROAD="MINIMUM_DISTANCE_OF_PROPERTY_LINE_FROM_THE_CENTRE_LINE_OF_THE_ROAD";
	public static final String TOTAL_CONNECTED_LOAD_OF_THE_PROPOSED_PROJECT_IN_W="TOTAL_CONNECTED_LOAD_OF_THE_PROPOSED_PROJECT_IN_W";
	public static final String MINIMUM_GENERATION_CAPACITY_OF_THE_ROOFTOP_SOLAR_PV_SYSTEM_IN_W="MINIMUM_GENERATION_CAPACITY_OF_THE_ROOFTOP_SOLAR_PV_SYSTEM_IN_W";
	public static final String CAPACITY_OF_SOLAR_WATER_HEATING_SYSTEM_IN_LPD="CAPACITY_OF_SOLAR_WATER_HEATING_SYSTEM_IN_LPD";
	
	public static final String G1="DOES_PROJECT_HAVE_LOW_WATER_CONSUMPTION_AND_PLUMBING_FIXTURES";
	public static final String G2="DOES_PROJECT_HAVE_REDUCED_HARDSCAPE";
	public static final String G3="DOES_PROJECT_HAVE_LOW_ENERGY_CONSUMPTION_LIGHTING_FIXTURES";
	public static final String G4="DOES_PROJECT_HAVE_ENERGY_EFFICIENT_HVAC_SYSTEM";
	public static final String G5="DOES_PROJECT_HAVE_LIGHTING_OF_COMMON_AREAS_BY_SOLAR_ENERGY_OR_LED_DEVICES";
	public static final String G6="DOES_PROJECT_HAVE_SEGREGATION_OF_WASTE_PROVISION";
	public static final String G7="DOES_PROJECT_HAVE_ORGANIC_WASTE_MANAGEMENT_PROVISION";
	
	public static final String IS_DRINKING_WATER_ON_EACH_FLOOR="IS_DRINKING_WATER_ON_EACH_FLOOR";
	public static final String NO_OF_OWNER_FOR_RESIDENTIAL_BUILDING="NO_OF_OWNER_FOR_RESIDENTIAL_BUILDING";
	public static final String No_Of_WARD="No_Of_WARD";
	
	public static final String IS_BOARDING_FACILITY_PRESENT="IS_BOARDING_FACILITY_PRESENT";
	
	public static final String BLT_UP_AREA_ERROR_MSG="Please cross verify *_BLT_UP_AREA layer";
	
	public static final String PROJECT_VALUE_IN_INR_IF_EIDP_FEE_IS_APPLICABLE_FOR_PROJECT="PROJECT_VALUE_IN_INR_IF_EIDP_FEE_IS_APPLICABLE_FOR_PROJECT";
	public static final String IS_THE_PROJECT_BY_STATE_GOVT_OR_CENTRAL_GOVT_OR_GOVT_UNDERTAKING="IS_THE_PROJECT_BY_STATE_GOVT_OR_CENTRAL_GOVT_OR_GOVT_UNDERTAKING";
	public static final String NUMBER_OF_TEMPORARY_STRUCTURES_IF_PRESENT_AT_THE_SITE="NUMBER_OF_TEMPORARY_STRUCTURES_IF_PRESENT_AT_THE_SITE";
	
	public static final String SHORTENED_SCRUTINY_REPORT = "shortenedReport";
	
	public static final String ADDITIONAL_TDR_IF_APPLICABLE_M2="ADDITIONAL_TDR_IF_APPLICABLE_M2";
	public static final String FEATURE_RESTRICTED_AREA = "Restricted area";
	public static final String SQM =" SQM";
	
	public static final String SETBACK_FRONT_EXISTING = "BLK_%s_SETBACK_FRONT_EXISTING"; 
	public static final String SETBACK_REAR_EXISTING = "BLK_%s_SETBACK_REAR_EXISTING"; 
	public static final String SETBACK_LEFT_EXISTING = "BLK_%s_SETBACK_LEFT_EXISTING"; 
	public static final String SETBACK_RIGHT_EXISTING = "BLK_%s_SETBACK_RIGHT_EXISTING"; 
	
	public static final String COLOUR_CODE_OF_PRINCIPAL_USE_OF_THE_BUILDING_IN_CASE_OF_MIXED_USE_PROJECTS = "COLOUR_CODE_OF_PRINCIPAL_USE_OF_THE_BUILDING_IN_CASE_OF_MIXED_USE_PROJECTS";
	
	public static final String ALTERATION_SERVICE_A = "ALTERATION_SERVICE_A";
	public static final String ALTERATION_SERVICE_B = "ALTERATION_SERVICE_B";
	public static final String ALTERATION_SERVICE_C = "ALTERATION_SERVICE_C";
	public static final String ALTERATION_SERVICE_D = "ALTERATION_SERVICE_D";
	
	// TODO: 11 constants.
	// City level customization for Puri code starts here.
	public static final String WHETHER_PLOT_FALLS_WITHIN_100_M_OF_GRAND_ROAD = "WHETHER_PLOT_FALLS_WITHIN_100_M_OF_GRAND_ROAD";
	public static final String WHETHER_PLOT_FALLS_WITHIN_3_KM_ARIAL_RADIUS_DISTANCE_OF_SRI_JAGANNATH_TEMPLE = "WHETHER_PLOT_FALLS_WITHIN_3_KM_ARIAL_RADIUS_DISTANCE_OF_SRI_JAGANNATH_TEMPLE";
	public static final String CRZ_NUMBER_FOR_PROJECTS_FALLING_UNDER_CRZ_AREA = "CRZ_NUMBER_FOR_PROJECTS_FALLING_UNDER_CRZ_AREA";
	// City level customization for Puri code ends here.

	public static final String IS_PROJECT_HAVING_SEWERAGE_CONNECTION = "IS_PROJECT_HAVING_SEWERAGE_CONNECTION";
	public static final String APPLICABLE = "Applicable";
	
	public static final String PERSQFTCOST = "PerSqFtCost";
}