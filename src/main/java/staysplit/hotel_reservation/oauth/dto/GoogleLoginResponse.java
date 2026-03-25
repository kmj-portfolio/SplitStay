package staysplit.hotel_reservation.oauth.dto;

public record GoogleLoginResponse(
        boolean needsSignup,
        String accessToken,
        String role,
        String socialId,
        String email,
        String name
) {
    public static GoogleLoginResponse loggedIn(String accessToken, String role) {
        return new GoogleLoginResponse(false, accessToken, role, null, null, null);
    }

    public static GoogleLoginResponse signupRequired(String socialId, String email, String name) {
        return new GoogleLoginResponse(true, null, null, socialId, email, name);
    }
}
