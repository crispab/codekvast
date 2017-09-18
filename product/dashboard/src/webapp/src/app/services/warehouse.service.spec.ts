import {DashboardService} from './dashboard.service';
import {ConfigService} from './config.service';
import {StateService} from './state.service';
import {Router} from '@angular/router';

let dashboard: DashboardService;

const configServiceMock: ConfigService = {
    getVersion() {
        return 'dev'
    },
    getApiPrefix() {
        return 'xxx'
    },
    isDemoMode() {
        return true;
    }
} as ConfigService;

const routerMock: Router = {
} as Router;

describe('DashboardService', () => {

    beforeEach(() => {
        dashboard = new DashboardService(null, configServiceMock, new StateService(), routerMock);
    });

    it('should construct a get methods url without parameters', () => {
        expect(dashboard.constructGetMethodsUrl(undefined, undefined)).toBe('xxx/webapp/v1/methods');
    });

    it('should construct a get methods url with only signature parameter', () => {
        expect(dashboard.constructGetMethodsUrl('sig', undefined)).toBe('xxx/webapp/v1/methods?signature=sig');
    });

    it('should construct a get methods url with signature parameter copied from IDEA with Copy Reference', () => {
        expect(dashboard.constructGetMethodsUrl('sample.app.SampleApp#run', undefined))
            .toBe('xxx/webapp/v1/methods?signature=sample.app.SampleApp.run');
    });

    it('should construct a get methods url with only blank signature parameter', () => {
        expect(dashboard.constructGetMethodsUrl(' ', undefined)).toBe('xxx/webapp/v1/methods');
    });

    it('should construct a get methods url with only maxResults parameter', () => {
        expect(dashboard.constructGetMethodsUrl(undefined, 100)).toBe('xxx/webapp/v1/methods?maxResults=100');
    });

    it('should construct a get methods url with blank signature and maxResults parameter', () => {
        expect(dashboard.constructGetMethodsUrl(' ', 100)).toBe('xxx/webapp/v1/methods?maxResults=100');
    });

    it('should construct a get methods url with both signature maxResults parameter', () => {
        expect(dashboard.constructGetMethodsUrl('sig', 100)).toBe('xxx/webapp/v1/methods?signature=sig&maxResults=100');
    });

    it('should construct a get methods url with a signature and null maxResults parameter', () => {
        expect(dashboard.constructGetMethodsUrl('sig', null)).toBe('xxx/webapp/v1/methods?signature=sig');
    });

    it('should construct a get methodById url ', () => {
        expect(dashboard.constructGetMethodByIdUrl(100)).toBe('xxx/webapp/v1/method/detail/100');
    });

});
