package projet.saik.survival;

/** Coordonnées téléphoniques d'urgence connues pour une destination. */
public record EmergencyInfo(
        String emergency,
        String police,
        String ambulance,
        String fire,
        String notes) {
}
