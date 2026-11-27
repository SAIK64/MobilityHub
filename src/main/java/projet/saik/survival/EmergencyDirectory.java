package projet.saik.survival;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import projet.saik.model.CountryIso;

/**
 * Référentiel local, sans appel réseau. Les valeurs sont des numéros nationaux
 * généraux ; elles doivent être revérifiées auprès des autorités avant le départ.
 */
public final class EmergencyDirectory {
    private EmergencyDirectory() {}

    private static final Map<CountryIso, EmergencyInfo> DATA;
    static {
        Map<CountryIso, EmergencyInfo> data = new LinkedHashMap<>();
        add(data, CountryIso.FR, "112", "17", "15", "18", "112 : numéro d'urgence européen");
        add(data, CountryIso.DE, "112", "110", "112", "112", "Police : 110 ; ambulance et pompiers : 112");
        add(data, CountryIso.ES, "112", "091", "061", "080", "112 : urgence générale");
        add(data, CountryIso.IT, "112", "112", "118", "115", "112 : numéro d'urgence général");
        add(data, CountryIso.PT, "112", "112", "112", "112", "Numéro européen d'urgence");
        add(data, CountryIso.GB, "999 / 112", "999 / 112", "999 / 112", "999 / 112", "999 ou 112");
        add(data, CountryIso.IE, "112 / 999", "112 / 999", "112 / 999", "112 / 999", "112 ou 999");
        add(data, CountryIso.BE, "112", "101", "112", "112", "Police : 101 ; ambulance et pompiers : 112");
        add(data, CountryIso.NL, "112", "112", "112", "112", "Numéro d'urgence général");
        add(data, CountryIso.CH, "112", "117", "144", "118", "112 fonctionne également ; police 117, ambulance 144, pompiers 118");
        add(data, CountryIso.AT, "112", "133", "144", "122", "Police 133, ambulance 144, pompiers 122");
        add(data, CountryIso.GR, "112", "100", "166", "199", "112 : urgence générale");
        add(data, CountryIso.US, "911", "911", "911", "911", "911 : urgence générale");
        add(data, CountryIso.CA, "911", "911", "911", "911", "911 : urgence générale");
        add(data, CountryIso.AU, "000", "000", "000", "000", "000 : urgence générale ; 112 depuis un mobile");
        add(data, CountryIso.NZ, "111", "111", "111", "111", "111 : urgence générale");
        add(data, CountryIso.JP, "110 / 119", "110", "119", "119", "Police 110 ; ambulance et pompiers 119");
        add(data, CountryIso.KR, "112 / 119", "112", "119", "119", "Police 112 ; ambulance et pompiers 119");
        add(data, CountryIso.TH, "191 / 1669", "191", "1669", "199", "Police 191 ; ambulance 1669 ; pompiers 199");
        add(data, CountryIso.MX, "911", "911", "911", "911", "911 : urgence générale");
        add(data, CountryIso.BR, "190 / 192 / 193", "190", "192", "193", "Police 190 ; ambulance 192 ; pompiers 193");
        add(data, CountryIso.MA, "19 / 15", "19", "15", "15", "Police 19 ; ambulance et pompiers 15 (selon zone)");
        add(data, CountryIso.TR, "112", "112", "112", "112", "112 : urgence générale");
        add(data, CountryIso.IN, "112", "112", "112", "112", "112 : urgence générale");
        add(data, CountryIso.SG, "999 / 995", "999", "995", "995", "Police 999 ; ambulance et pompiers 995");
        add(data, CountryIso.AE, "999 / 998 / 997", "999", "998", "997", "Police 999 ; ambulance 998 ; pompiers 997");
        DATA = Collections.unmodifiableMap(data);
    }

    private static void add(Map<CountryIso, EmergencyInfo> map, CountryIso iso,
            String emergency, String police, String ambulance, String fire, String notes) {
        map.put(iso, new EmergencyInfo(emergency, police, ambulance, fire, notes));
    }

    public static Map<CountryIso, EmergencyInfo> all() { return DATA; }
    public static EmergencyInfo find(CountryIso iso) { return DATA.get(iso); }
}
