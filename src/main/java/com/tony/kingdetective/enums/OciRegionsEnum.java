package com.tony.kingdetective.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Tony Wang
 */

public enum OciRegionsEnum {

    // Asia Pacific
    AP_SYDNEY_1("ap-sydney-1", "??????????", "Sydney, Australia", "SYD"),
    AP_MELBOURNE_1("ap-melbourne-1", "????????????", "Melbourne, Australia", "MEL"),
    AP_HYDERABAD_1("ap-hyderabad-1", "??????????", "Hyderabad, India", "HYD"),
    AP_MUMBAI_1("ap-mumbai-1", "????????", "Mumbai, India", "BOM"),
    AP_BATAM_1("ap-batam-1", "????????????", "Batam, Indonesia", "HSG"),
    AP_OSAKA_1("ap-osaka-1", "????????", "Osaka, Japan", "KIX"),
    AP_TOKYO_1("ap-tokyo-1", "????????", "Tokyo, Japan", "NRT"),
    AP_SINGAPORE_1("ap-singapore-1", "???"?, "Singapore", "SIN"),
    AP_SINGAPORE_2("ap-singapore-2", "?????"?, "Singapore", "XSP"),
    AP_SEOUL_1("ap-seoul-1", "????????", "Seoul, South Korea", "ICN"),
    AP_CHUNCHEON_1("ap-chuncheon-1", "????????", "Chuncheon, South Korea", "YNY"),

    // Americas
    SA_SAOPAULO_1("sa-saopaulo-1", "?????????"?, "Sao Paulo, Brazil", "GRU"),
    SA_VINHEDO_1("sa-vinhedo-1", "??????????", "Vinhedo, Brazil", "VCP"),
    CA_MONTREAL_1("ca-montreal-1", "????????????", "Montreal, Canada", "YUL"),
    CA_TORONTO_1("ca-toronto-1", "???????????"?, "Toronto, Canada", "YYZ"),
    SA_SANTIAGO_1("sa-santiago-1", "??????????", "Santiago, Chile", "SCL"),
    SA_VALPARAISO_1("sa-valparaiso-1", "???????????"?, "Valparaiso, Chile", "VAP"),
    SA_BOGOTA_1("sa-bogota-1", "???????????"?, "Bogota, Colombia", "BOG"),
    MX_QUERETARO_1("mx-queretaro-1", "???????????"?, "Queretaro, Mexico", "QRO"),
    MX_MONTERREY_1("mx-monterrey-1", "???????????"?, "Monterrey, Mexico", "MTY"),
    US_ASHBURN_1("us-ashburn-1", "?????????", "Ashburn, VA", "IAD"),
    US_CHICAGO_1("us-chicago-1", "??????????", "Chicago, IL", "ORD"),
    US_PHOENIX_1("us-phoenix-1", "?????????"?, "Phoenix, AZ", "PHX"),
    US_SANJOSE_1("us-sanjose-1", "?????????"?, "San Jose, CA", "SJC"),

    // Europe
    EU_PARIS_1("eu-paris-1", "????????", "Paris, France", "CDG"),
    EU_MARSEILLE_1("eu-marseille-1", "????????", "Marseille, France", "MRS"),
    EU_FRANKFURT_1("eu-frankfurt-1", "??????????", "Frankfurt, Germany", "FRA"),
    EU_MILAN_1("eu-milan-1", "??????????", "Milan, Italy", "LIN"),
    EU_TURIN_1("eu-turin-1", "?????????"?, "Turin, Italy", "NRQ"),
    EU_AMSTERDAM_1("eu-amsterdam-1", "????????????", "Amsterdam, Netherlands", "AMS"),
    EU_JOVANOVAC_1("eu-jovanovac-1", "?????????????"?, "Jovanovac, Serbia", "BEG"),
    EU_MADRID_1("eu-madrid-1", "??????????", "Madrid, Spain", "MAD"),
    EU_MADRID_3("eu-madrid-3", "?????????"??, "Madrid, Spain", "ORF"),
    EU_STOCKHOLM_1("eu-stockholm-1", "???????????"?, "Stockholm, Sweden", "ARN"),
    EU_ZURICH_1("eu-zurich-1", "?????????"?, "Zurich, Switzerland", "ZRH"),
    UK_LONDON_1("uk-london-1", "????????", "London, United Kingdom", "LHR"),
    UK_CARDIFF_1("uk-cardiff-1", "?????????"?, "Newport, United Kingdom", "CWL"),

    // Middle East & Africa
    IL_JERUSALEM_1("il-jerusalem-1", "???????????"?, "Jerusalem, Israel", "MTZ"),
    ME_RIYADH_1("me-riyadh-1", "????????????", "Riyadh, Saudi Arabia", "RUH"),
    ME_JEDDAH_1("me-jeddah-1", "???????????"?, "Jeddah, Saudi Arabia", "JED"),
    AF_JOHANNESBURG_1("af-johannesburg-1", "???????????"?, "Johannesburg, South Africa", "JNB"),
    ME_ABUDHABI_1("me-abudhabi-1", "???????????"?, "Abu Dhabi, UAE", "AUH"),
    ME_DUBAI_1("me-dubai-1", "?????????"?, "Dubai, UAE", "DXB");

    private final String id;
    private final String name;
    private final String location;
    private final String key;

    OciRegionsEnum(String id, String name, String location, String key) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getKey() {
        return key;
    }

    /**
     *  region id  key
     */
    public static Optional<String> getKeyById(String id) {
        return Arrays.stream(values())
                .filter(r -> r.id.equalsIgnoreCase(id))
                .map(OciRegionsEnum::getKey)
                .findFirst();
    }

    /**
     *  region id  name
     */
    public static Optional<String> getNameById(String id) {
        return Arrays.stream(values())
                .filter(r -> r.id.equalsIgnoreCase(id))
                .map(OciRegionsEnum::getName)
                .findFirst();
    }
}
