'use strict';

describe('JumbotronController', function () {

    // load the controller's module
    beforeEach(module('codekvastApp'));

    var JumbotronController,
        RemoteDataService,
        scope,
        now = Date.now();

    // Initialize the controller and a mock scope
    beforeEach(inject(function ($controller, $rootScope, _RemoteDataService_) {
        scope = $rootScope.$new();
        RemoteDataService = _RemoteDataService_;
        JumbotronController = $controller('JumbotronController', {
            $scope: scope
        });
    }));

    it('should have an undefined list of signatures', function () {
        expect(scope.signatures).toBeUndefined;
    });

    it('should have an undefined collectorStatus', function () {
        expect(scope.collectorStatus).toBeUndefined();
    });

});
