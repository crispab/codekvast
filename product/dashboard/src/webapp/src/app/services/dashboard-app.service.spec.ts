import {ConfigService} from './config.service';
import {DashboardAppService, GetMethodsRequest} from './dashboard-app.service';

let app: DashboardAppService;

const configServiceMock: ConfigService = {
    getVersion() {
        return 'dev'
    },
    isDemoMode() {
        return true;
    }
} as ConfigService;

describe('DashboardAppService', () => {

    beforeEach(() => {
        app = new DashboardAppService(null, configServiceMock);
    });

    it('should construct a get methods url with signature containing wildcard', () => {
        expect(app.constructGetMethodsUrl({signature: 'sample.app.SampleApp%foo*bar'} as GetMethodsRequest))
            .toBe('/webapp/v1/methods?signature=sample.app.SampleApp%25foo*bar');
    });

});
