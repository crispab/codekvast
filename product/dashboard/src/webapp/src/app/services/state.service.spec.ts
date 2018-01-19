import {StateService} from './state.service';

let stateService: StateService;

describe('StateService', () => {

    beforeEach(() => {
        stateService = new StateService();
        stateService.setLoggedOut();
    });

    it('Should be in demo mode by default', () => {
        expect(stateService.isDemoMode()).toBe(true);
    });

    it('Should respect setDemoMode(false)', () => {
        stateService.setDemoMode(false);
        expect(stateService.isDemoMode()).toBe(false);
    });

    it('Should say "Demo mode" when in demo mode', () => {
        expect(stateService.getLoginState()).toBe('Demo mode');
    });

    it('Should say "Not logged in" when not in demo mode and not logged in', () => {
        stateService.setDemoMode(false);
        expect(stateService.getLoginState()).toBe('Not logged in');
    });

    it('Should not be logged in by default', () => {
        expect(stateService.isLoggedIn()).toBe(false);
    });

    it('Should accept setLoggedInAs()', () => {
        stateService.setDemoMode(false);
        stateService.setLoggedInAs('customerName', 'foo@bar.baz', 'heroku', 'my-heroku-app');
        expect(stateService.isLoggedIn()).toBe(true);
        expect(stateService.getLoginState()).toBe('Logged in as foo@bar.baz');
    });

    it('Should accept setLoggedOut()', () => {
        stateService.setDemoMode(false);
        stateService.setLoggedInAs('customerName', 'foo@bar.baz', 'heroku', 'my-heroku-app');
        expect(stateService.isLoggedIn()).toBe(true);

        stateService.setLoggedOut();

        expect(stateService.getLoginState()).toBe('Not logged in');
    });
});
