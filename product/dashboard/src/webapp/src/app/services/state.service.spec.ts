import {StateService} from './state.service';

let stateService: StateService;

describe('StateService', () => {

    beforeEach(() => {
        stateService = new StateService(null);
        stateService.setLoggedOut();
    });

    it('Should not be logged in by default', () => {
        expect(stateService.isLoggedIn()).toBe(false);
    });

    it('Should accept setLoggedInAs()', () => {
        stateService.setLoggedInAs('customerName', 'foo@bar.baz', 'heroku', 'my-heroku-app');
        expect(stateService.isLoggedIn()).toBe(true);
    });

    it('Should accept setLoggedOut()', () => {
        stateService.setLoggedInAs('customerName', 'foo@bar.baz', 'heroku', 'my-heroku-app');
        expect(stateService.isLoggedIn()).toBe(true);
        stateService.setLoggedOut();
        expect(stateService.isLoggedIn()).toBe(false);
    });
});
