package projet.saik.dto;

public record TokenResponse(String token, String type, long expiresInSeconds) {
}
