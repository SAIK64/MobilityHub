package projet.saik.service;

public class EmailAlreadyUseException extends RuntimeException {
    public EmailAlreadyUseException(String email) {
        super("Email déjà utilisé : " + email);
    }
}
