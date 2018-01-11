import {DashboardService, GetMethodsRequest} from './dashboard.service';
import {ConfigService} from './config.service';
import {StateService} from './state.service';

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

describe('DashboardService', () => {

    beforeEach(() => {
        dashboard = new DashboardService(null, configServiceMock, new StateService());
    });

    it('should construct a get methods url with signature containing wildcard', () => {
        expect(dashboard.constructGetMethodsUrl({signature: 'sample.app.SampleApp%foo*bar'} as GetMethodsRequest))
            .toBe('xxx/webapp/v1/methods?signature=sample.app.SampleApp%25foo*bar');
    });

});
