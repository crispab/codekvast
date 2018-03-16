import {StateService} from './state.service';
import {CookieService} from 'ngx-cookie';

let stateService: StateService;

const cookieServiceMock: CookieService = {
    remove: function (key: string) {
        console.log('Removing cookie ' + key);
    }
} as CookieService;

describe('StateService', () => {

    beforeEach(() => {
        stateService = new StateService(cookieServiceMock);
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
