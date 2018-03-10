import {StateService} from './state.service';

let stateService: StateService;

describe('StateService', () => {

    beforeEach(() => {
        stateService = new StateService();
        stateService.setLoggedOut();
    });

    it('Should say "Not logged in" when not logged in', () => {
        expect(stateService.getLoginState()).toBe('Not logged in');
    });

    it('Should not be logged in by default', () => {
        expect(stateService.isLoggedIn()).toBe(false);
    });

    it('Should accept setLoggedInAs()', () => {
        stateService.setLoggedInAs('customerName', 'foo@bar.baz', 'heroku', 'my-heroku-app');
        expect(stateService.isLoggedIn()).toBe(true);
        expect(stateService.getLoginState()).toBe('Logged in as foo@bar.baz');
    });

    it('Should accept setLoggedOut()', () => {
        stateService.setLoggedInAs('customerName', 'foo@bar.baz', 'heroku', 'my-heroku-app');
        expect(stateService.isLoggedIn()).toBe(true);

        stateService.setLoggedOut();

        expect(stateService.getLoginState()).toBe('Not logged in');
    });
});
