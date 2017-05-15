/**
 * The state for Authentication.
 */
export class AuthState {
    static KEY = 'auth';

    authToken: string;

    setAuthToken(token: string, navData: string) {
        this.authToken = token;
        if (navData) {
            window['Boomerang'].init(navData);
        }
    }

}
