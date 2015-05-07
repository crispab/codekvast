'use strict';

describe('RemoteDataService', function () {

    // load the controller's module
    beforeEach(module('codekvastApp'));

    var RemoteDataService;

    // Initialize the controller and a mock scope
    beforeEach(inject(function (_RemoteDataService_) {
        RemoteDataService = _RemoteDataService_;
    }));

    it('should create unique list of versions', function () {
        RemoteDataService.handleWebSocketMessage(
            {
                applicationStatistics: [
                    { name: 'n1', version: '2.10'},
                    { name: 'n2', version: '10.2'},
                    { name: 'n3', version: '2.2'},
                    { name: 'n4', version: '10.11'},
                    { name: 'n41', version: '2.2-beta'},
                    { name: 'n5', version: '10.2'},
                    { name: 'n6', version: '10.11'},
                    { name: 'n7', version: '2.10-alpha'}
                ]
            }
        )
        expect(RemoteDataService.getLastData('versions')).toEqual([{name: '2.2'}, {name: '2.2-beta'}, {name: '2.10'}, {name: '2.10-alpha'}, {name: '10.2'}, {name: '10.11'}])
    });

});
