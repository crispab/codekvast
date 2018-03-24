import {DashboardApiService, GetMethodsRequest} from './dashboard-api.service';

let api: DashboardApiService;

describe('DashboardApiService', () => {

    beforeEach(() => {
        api = new DashboardApiService(null);
    });

    it('should construct a get methods url with signature containing wildcard', () => {
        expect(api.constructGetMethodsUrl({signature: 'sample.app.SampleApp%foo*bar'} as GetMethodsRequest))
            .toBe('/dashboard/api/v1/methods?signature=sample.app.SampleApp%25foo*bar');
    });

});
