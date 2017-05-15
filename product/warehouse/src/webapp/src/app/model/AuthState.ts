/**
 * The state for Authentication.
 */
export class AuthState {
    static KEY = 'auth';

    authToken: string;

    setAuthToken(token: string) {
        this.authToken = token;
    }

}
