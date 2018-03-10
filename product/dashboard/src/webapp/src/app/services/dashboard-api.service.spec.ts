import {ConfigService} from './config.service';
import {DashboardApiService, GetMethodsRequest} from './dashboard-api.service';

let api: DashboardApiService;

const configServiceMock: ConfigService = {
    getVersion() {
        return 'dev'
    },
} as ConfigService;

describe('DashboardApiService', () => {

    beforeEach(() => {
        api = new DashboardApiService(null, configServiceMock);
    });

    it('should construct a get methods url with signature containing wildcard', () => {
        expect(api.constructGetMethodsUrl({signature: 'sample.app.SampleApp%foo*bar'} as GetMethodsRequest))
            .toBe('/webapp/v1/methods?signature=sample.app.SampleApp%25foo*bar');
    });

});
